package com.cloudcostai.controller;

import com.cloudcostai.dto.ApiResponse;
import com.cloudcostai.dto.HealthResponse;
import com.cloudcostai.service.HealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health check controller for uptime and component connectivity verification.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Health & Diagnostics", description = "Operational health and probe endpoints")
public class HealthController {

    private final HealthService healthService;

    @Autowired
    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping("/health")
    @Operation(summary = "Application Health Probe", description = "Checks the health of the application, MySQL database connectivity, and system resources")
    public ResponseEntity<ApiResponse<HealthResponse>> checkHealth() {
        HealthResponse health = healthService.getHealth();

        if ("DOWN".equalsIgnoreCase(health.getStatus())) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error("Service is degraded or unavailable"));
        }

        return ResponseEntity.ok(ApiResponse.success("System operational", health));
    }
}
