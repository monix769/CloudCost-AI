package com.cloudcostai.controller;

import com.cloudcostai.anomaly.service.AnomalyDetectionEngineService;
import com.cloudcostai.dto.AnomalyConfigRequest;
import com.cloudcostai.dto.AnomalyConfigResponse;
import com.cloudcostai.dto.AnomalyDetectionResult;
import com.cloudcostai.dto.AnomalyHistoryResponse;
import com.cloudcostai.dto.AnomalyKpiSummaryResponse;
import com.cloudcostai.dto.AnomalyStatusUpdateRequest;
import com.cloudcostai.dto.AnomalyTrendsResponse;
import com.cloudcostai.dto.ApiResponse;
import com.cloudcostai.dto.CostAnomalyResponse;
import com.cloudcostai.entity.AnomalySeverity;
import com.cloudcostai.entity.AnomalyStatus;
import com.cloudcostai.entity.CloudProvider;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.service.AnomalyConfigService;
import com.cloudcostai.service.AnomalyService;
import com.cloudcostai.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/anomalies")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Anomaly Management", description = "Endpoints for inspecting detected cost anomalies, statistical parameters, and workflow transitions")
public class AnomalyController {

    private final AnomalyService anomalyService;
    private final AnomalyConfigService anomalyConfigService;
    private final AnomalyDetectionEngineService detectionEngineService;
    private final OrganizationService organizationService;

    @GetMapping
    @Operation(summary = "List cost anomalies", description = "Retrieve detected cost anomalies with optional filters and pagination")
    public ResponseEntity<ApiResponse<?>> getAnomalies(
            @Parameter(description = "Filter by severity (LOW, MEDIUM, HIGH, CRITICAL)")
            @RequestParam(required = false) AnomalySeverity severity,
            @Parameter(description = "Filter by status (OPEN, ACKNOWLEDGED, RESOLVED, DISMISSED, IGNORED)")
            @RequestParam(required = false) AnomalyStatus status,
            @Parameter(description = "Filter by cloud provider (AWS, AZURE, GCP)")
            @RequestParam(required = false) CloudProvider provider,
            @Parameter(description = "Filter by service name (case-insensitive substring)")
            @RequestParam(required = false) String serviceName,
            @Parameter(description = "Start date filter (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date filter (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Page number (0-indexed, optional)")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size (default 20, max 100)")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort direction (asc, desc)")
            @RequestParam(defaultValue = "desc") String direction) {

        log.debug("GET /api/anomalies - severity: {}, status: {}, provider: {}, service: {}, page: {}",
                severity, status, provider, serviceName, page);

        if (page != null) {
            Sort sort = Sort.by("asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC, "detectedAt");
            Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), sort);
            Page<CostAnomalyResponse> paged = anomalyService.getFilteredAnomaliesPaged(
                    provider, serviceName, severity, status, startDate, endDate, pageable);
            return ResponseEntity.ok(ApiResponse.success(paged));
        }

        List<CostAnomalyResponse> list = anomalyService.getFilteredAnomaliesList(
                provider, serviceName, severity, status, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get anomaly by ID", description = "Retrieve a single anomaly record and root-cause details")
    public ResponseEntity<ApiResponse<CostAnomalyResponse>> getAnomalyById(
            @Parameter(description = "Anomaly ID") @PathVariable Long id) {
        log.debug("GET /api/anomalies/{}", id);
        CostAnomalyResponse anomaly = anomalyService.getAnomalyById(id);
        return ResponseEntity.ok(ApiResponse.success(anomaly));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get anomaly KPI summary", description = "Retrieve aggregate metrics including critical count and potential excess spend")
    public ResponseEntity<ApiResponse<AnomalyKpiSummaryResponse>> getAnomalySummary(
            @Parameter(description = "Target currency code (e.g. USD)")
            @RequestParam(required = false) String currency) {
        log.debug("GET /api/anomalies/summary - currency: {}", currency);
        AnomalyKpiSummaryResponse summary = anomalyService.getAnomalyKpiSummary(currency);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/trends")
    @Operation(summary = "Get anomaly trends", description = "Time-series daily counts of detected cost anomalies and excess spending")
    public ResponseEntity<ApiResponse<AnomalyTrendsResponse>> getAnomalyTrends(
            @Parameter(description = "Start date (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Target currency code (e.g. USD)")
            @RequestParam(required = false) String currency) {
        log.debug("GET /api/anomalies/trends - start: {}, end: {}", startDate, endDate);
        AnomalyTrendsResponse trends = anomalyService.getAnomalyTrends(startDate, endDate, currency);
        return ResponseEntity.ok(ApiResponse.success(trends));
    }

    @PostMapping("/detect")
    @Operation(summary = "Trigger anomaly detection scan", description = "Manually execute statistical anomaly detectors across all accounts and budgets")
    public ResponseEntity<ApiResponse<AnomalyDetectionResult>> triggerDetection(
            @Parameter(description = "Optional evaluation target date (defaults to today)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate) {
        Organization org = organizationService.getCurrentUserOrganization();
        log.info("POST /api/anomalies/detect triggered for organization [{}] on date [{}]", org.getId(), targetDate);
        AnomalyDetectionResult result = detectionEngineService.runDetectionForOrganization(org.getId(), targetDate);
        return ResponseEntity.ok(ApiResponse.success(result.getSummaryMessage(), result));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update anomaly status", description = "Transition the lifecycle state of an anomaly (e.g. acknowledge, resolve, dismiss)")
    public ResponseEntity<ApiResponse<CostAnomalyResponse>> updateAnomalyStatus(
            @Parameter(description = "Anomaly ID") @PathVariable Long id,
            @Valid @RequestBody AnomalyStatusUpdateRequest request) {
        log.info("PATCH /api/anomalies/{}/status -> {} (notes: {})", id, request.getStatus(), request.getNotes());
        CostAnomalyResponse updated = anomalyService.updateAnomalyStatus(id, request.getStatus(), request.getNotes());
        return ResponseEntity.ok(ApiResponse.success("Anomaly status transitioned successfully", updated));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Get anomaly history", description = "Retrieve audit trail of all lifecycle status transitions for an anomaly")
    public ResponseEntity<ApiResponse<List<AnomalyHistoryResponse>>> getAnomalyHistory(
            @Parameter(description = "Anomaly ID") @PathVariable Long id) {
        log.debug("GET /api/anomalies/{}/history", id);
        List<AnomalyHistoryResponse> history = anomalyService.getAnomalyHistory(id);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/config")
    @Operation(summary = "Get detection configuration", description = "Inspect current statistical thresholds and parameters for the organization")
    public ResponseEntity<ApiResponse<AnomalyConfigResponse>> getDetectionConfig() {
        log.debug("GET /api/anomalies/config");
        AnomalyConfigResponse config = anomalyConfigService.getConfig();
        return ResponseEntity.ok(ApiResponse.success(config));
    }

    @PutMapping("/config")
    @Operation(summary = "Update detection configuration", description = "Customize Z-score thresholds, moving average window, and spike percentages")
    public ResponseEntity<ApiResponse<AnomalyConfigResponse>> updateDetectionConfig(
            @Valid @RequestBody AnomalyConfigRequest request) {
        log.info("PUT /api/anomalies/config");
        AnomalyConfigResponse updated = anomalyConfigService.updateConfig(request);
        return ResponseEntity.ok(ApiResponse.success("Anomaly detection configuration updated successfully", updated));
    }
}
