package com.cloudcostai.controller;

import com.cloudcostai.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Root API discovery controller.
 */
@RestController
@Tag(name = "Root Discovery", description = "Base platform discovery endpoint")
public class RootController {

    @GetMapping("/")
    @Operation(summary = "Root API Information", description = "Returns platform metadata, version, and primary endpoint discovery links")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRootInfo() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("platform", "CloudCost AI");
        data.put("title", "Intelligent Cloud Cost Monitoring & FinOps Analytics Platform");
        data.put("version", "1.0.0-SNAPSHOT");
        data.put("status", "ONLINE");
        data.put("currentMilestone", "Milestone 1 - Foundation & Architecture");
        data.put("documentationUrl", "/swagger-ui.html");
        data.put("openApiSpecUrl", "/v3/api-docs");

        Map<String, String> endpoints = new LinkedHashMap<>();
        endpoints.put("root", "GET /");
        endpoints.put("health", "GET /api/health");
        endpoints.put("status", "GET /api/status");
        endpoints.put("swaggerUi", "GET /swagger-ui.html");
        endpoints.put("apiDocs", "GET /v3/api-docs");
        data.put("endpoints", endpoints);

        data.put("serverTime", Instant.now());

        return ResponseEntity.ok(ApiResponse.success("Welcome to CloudCost AI FinOps Platform", data));
    }
}
