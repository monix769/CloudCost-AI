package com.cloudcostai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Health check DTO returning the operational status of the service and components.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Health status payload")
public class HealthResponse {

    @Schema(description = "Overall application status", example = "UP")
    private String status;

    @Schema(description = "MySQL Database connection status", example = "CONNECTED")
    private String database;

    @Schema(description = "Platform version", example = "1.0.0-SNAPSHOT")
    private String version;

    @Schema(description = "Uptime in seconds", example = "120")
    private long uptimeSeconds;

    @Schema(description = "Component health details")
    private Map<String, Object> components;

    @Schema(description = "Server timestamp")
    private Instant timestamp;
}
