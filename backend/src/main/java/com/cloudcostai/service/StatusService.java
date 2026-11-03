package com.cloudcostai.service;

import com.cloudcostai.dto.StatusResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service providing detailed architectural and runtime status information.
 */
@Service
public class StatusService {

    private final Environment environment;

    @Autowired
    public StatusService(Environment environment) {
        this.environment = environment;
    }

    public StatusResponse getSystemStatus() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        Map<String, Object> memoryStats = new HashMap<>();
        memoryStats.put("maxMemoryMb", maxMemory / (1024 * 1024));
        memoryStats.put("totalMemoryMb", totalMemory / (1024 * 1024));
        memoryStats.put("usedMemoryMb", usedMemory / (1024 * 1024));
        memoryStats.put("freeMemoryMb", freeMemory / (1024 * 1024));

        String[] activeProfiles = environment.getActiveProfiles();
        List<String> profilesList = activeProfiles.length == 0
                ? List.of("default")
                : Arrays.asList(activeProfiles);

        String envName = profilesList.contains("prod") ? "production" : "development";

        return StatusResponse.builder()
                .application("CloudCost AI - FinOps Analytics Platform")
                .environment(envName)
                .activeProfiles(profilesList)
                .javaVersion(System.getProperty("java.version"))
                .springBootVersion(org.springframework.boot.SpringBootVersion.getVersion())
                .securityMode("STATELESS_PERMIT_ALL_M1")
                .completedMilestones(List.of(
                        "Milestone 1: Complete Project Foundation & Architecture"
                ))
                .activeModules(List.of(
                        "REST API Engine (Spring Web)",
                        "MySQL Persistence Layer (Spring Data JPA / Hibernate)",
                        "Swagger OpenAPI 3 Documentation Engine",
                        "Security Architecture Layer (Spring Security 6)",
                        "Centralized Exception Handling",
                        "Health & Actuator Metrics Monitor",
                        "React.js + Vite Client Integration"
                ))
                .memory(memoryStats)
                .timestamp(Instant.now())
                .build();
    }
}
