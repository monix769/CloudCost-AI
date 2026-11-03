package com.cloudcostai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Detailed system status DTO including environment and runtime metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detailed system status and runtime environment payload")
public class StatusResponse {

    @Schema(description = "Application name", example = "CloudCost AI")
    private String application;

    @Schema(description = "Environment identifier", example = "development")
    private String environment;

    @Schema(description = "Active Spring profiles", example = "[\"dev\"]")
    private List<String> activeProfiles;

    @Schema(description = "Java runtime version", example = "21.0.6")
    private String javaVersion;

    @Schema(description = "Spring Boot version", example = "3.3.5")
    private String springBootVersion;

    @Schema(description = "Security configuration mode", example = "STATELESS_PERMIT_ALL_M1")
    private String securityMode;

    @Schema(description = "Platform milestones completed", example = "[\"Milestone 1 - Foundation & Architecture\"]")
    private List<String> completedMilestones;

    @Schema(description = "Active system modules", example = "[\"Health Monitoring\", \"Status Reporting\", \"OpenAPI/Swagger\", \"MySQL Connectivity\"]")
    private List<String> activeModules;

    @Schema(description = "JVM memory usage statistics")
    private Map<String, Object> memory;

    @Schema(description = "Server timestamp")
    private Instant timestamp;
}
