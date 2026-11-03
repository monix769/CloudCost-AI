package com.cloudcostai.service;

import com.cloudcostai.dto.HealthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for computing overall application health.
 */
@Service
public class HealthService {

    private final DatabaseHealthIndicatorService databaseHealthIndicatorService;
    private final BuildProperties buildProperties;

    @Autowired
    public HealthService(DatabaseHealthIndicatorService databaseHealthIndicatorService,
                         @Autowired(required = false) BuildProperties buildProperties) {
        this.databaseHealthIndicatorService = databaseHealthIndicatorService;
        this.buildProperties = buildProperties;
    }

    public HealthResponse getHealth() {
        Map<String, Object> dbHealth = databaseHealthIndicatorService.checkDatabaseHealth();
        boolean dbOk = "UP".equals(dbHealth.get("status"));

        String appStatus = dbOk ? "UP" : "DEGRADED";
        String dbStatus = dbOk ? "CONNECTED" : "DISCONNECTED";

        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        long uptimeSeconds = runtimeBean.getUptime() / 1000;

        Map<String, Object> components = new HashMap<>();
        components.put("database", dbHealth);

        Map<String, Object> diskSpace = new HashMap<>();
        java.io.File root = new java.io.File(".");
        diskSpace.put("status", "UP");
        diskSpace.put("freeBytes", root.getFreeSpace());
        diskSpace.put("totalBytes", root.getTotalSpace());
        components.put("diskSpace", diskSpace);

        String version = buildProperties != null ? buildProperties.getVersion() : "1.0.0-SNAPSHOT";

        return HealthResponse.builder()
                .status(appStatus)
                .database(dbStatus)
                .version(version)
                .uptimeSeconds(uptimeSeconds)
                .components(components)
                .timestamp(Instant.now())
                .build();
    }
}
