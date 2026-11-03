package com.cloudcostai.controller;

import com.cloudcostai.dto.AnomalyStatusUpdateRequest;
import com.cloudcostai.dto.ApiResponse;
import com.cloudcostai.dto.CostAnomalyResponse;
import com.cloudcostai.entity.AnomalySeverity;
import com.cloudcostai.entity.AnomalyStatus;
import com.cloudcostai.service.AnomalyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/anomalies")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Anomaly Management", description = "Endpoints for inspecting detected cost anomalies and updating resolution workflow states")
public class AnomalyController {

    private final AnomalyService anomalyService;

    @GetMapping
    @Operation(summary = "List cost anomalies", description = "Retrieve detected cost anomalies, with optional filters for severity and lifecycle status")
    public ResponseEntity<ApiResponse<List<CostAnomalyResponse>>> getAnomalies(
            @Parameter(description = "Filter by severity (LOW, MEDIUM, HIGH, CRITICAL)")
            @RequestParam(required = false) AnomalySeverity severity,
            @Parameter(description = "Filter by status (OPEN, ACKNOWLEDGED, RESOLVED, IGNORED)")
            @RequestParam(required = false) AnomalyStatus status) {
        log.debug("GET /api/anomalies - severity: {}, status: {}", severity, status);
        List<CostAnomalyResponse> anomalies = anomalyService.getAnomalies(severity, status);
        return ResponseEntity.ok(ApiResponse.success(anomalies));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get anomaly by ID", description = "Retrieve a single anomaly record and root-cause details")
    public ResponseEntity<ApiResponse<CostAnomalyResponse>> getAnomalyById(
            @Parameter(description = "Anomaly ID") @PathVariable Long id) {
        log.debug("GET /api/anomalies/{}", id);
        CostAnomalyResponse anomaly = anomalyService.getAnomalyById(id);
        return ResponseEntity.ok(ApiResponse.success(anomaly));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update anomaly status", description = "Transition the lifecycle state of an anomaly (e.g. acknowledge, resolve, ignore)")
    public ResponseEntity<ApiResponse<CostAnomalyResponse>> updateAnomalyStatus(
            @Parameter(description = "Anomaly ID") @PathVariable Long id,
            @Valid @RequestBody AnomalyStatusUpdateRequest request) {
        log.info("PATCH /api/anomalies/{}/status -> {}", id, request.getStatus());
        CostAnomalyResponse updated = anomalyService.updateAnomalyStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Anomaly status transitioned successfully", updated));
    }
}
