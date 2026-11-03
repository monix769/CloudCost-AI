package com.cloudcostai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Service responsible for probing and verifying MySQL database connectivity.
 */
@Service
public class DatabaseHealthIndicatorService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseHealthIndicatorService.class);

    private final DataSource dataSource;

    @Autowired
    public DatabaseHealthIndicatorService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Map<String, Object> checkDatabaseHealth() {
        Map<String, Object> details = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            DatabaseMetaData metaData = connection.getMetaData();
            details.put("status", "UP");
            details.put("databaseProduct", metaData.getDatabaseProductName());
            details.put("databaseVersion", metaData.getDatabaseProductVersion());
            details.put("driverName", metaData.getDriverName());
            details.put("catalog", connection.getCatalog());

            try (ResultSet rs = statement.executeQuery("SELECT 1")) {
                if (rs.next()) {
                    long latency = System.currentTimeMillis() - startTime;
                    details.put("pingLatencyMs", latency);
                    details.put("connectionValid", true);
                }
            }

        } catch (Exception ex) {
            log.error("Database health check failed: {}", ex.getMessage());
            details.put("status", "DOWN");
            details.put("error", ex.getMessage());
            details.put("connectionValid", false);
        }

        return details;
    }

    public boolean isConnected() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT 1")) {
            return rs.next();
        } catch (Exception e) {
            return false;
        }
    }
}
