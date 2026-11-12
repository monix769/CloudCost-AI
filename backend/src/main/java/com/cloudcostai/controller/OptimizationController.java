package com.cloudcostai.controller;

import com.cloudcostai.dto.ApiResponse;
import com.cloudcostai.dto.CommitmentAnalysisResponse;
import com.cloudcostai.dto.OptimizationOverviewResponse;
import com.cloudcostai.dto.OptimizationRecommendationHistoryResponse;
import com.cloudcostai.dto.OptimizationRecommendationResponse;
import com.cloudcostai.dto.OptimizationStatusUpdateRequest;
import com.cloudcostai.dto.ResourceUtilizationResponse;
import com.cloudcostai.dto.SavingsSummaryResponse;
import com.cloudcostai.entity.CloudProvider;
import com.cloudcostai.entity.OptimizationPriority;
import com.cloudcostai.entity.OptimizationRecommendationType;
import com.cloudcostai.entity.OptimizationStatus;
import com.cloudcostai.service.OptimizationService;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/optimization")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cost Optimization & Rightsizing", description = "Endpoints for intelligent cloud rightsizing, idle resource detection, commitment planning, and lifecycle tracking")
public class OptimizationController {

    private final OptimizationService optimizationService;

    @GetMapping("/overview")
    @Operation(summary = "Get optimization overview KPIs", description = "Retrieve summary metrics including total potential and realized savings")
    public ResponseEntity<ApiResponse<OptimizationOverviewResponse>> getOverview() {
        log.debug("GET /api/optimization/overview");
        OptimizationOverviewResponse overview = optimizationService.getOverview();
        return ResponseEntity.ok(ApiResponse.success(overview));
    }

    @GetMapping("/recommendations")
    @Operation(summary = "List recommendations", description = "Retrieve filterable paginated rightsizing and cost optimization recommendations")
    public ResponseEntity<ApiResponse<Page<OptimizationRecommendationResponse>>> getRecommendations(
            @Parameter(description = "Filter by cloud provider (AWS, AZURE, GCP)")
            @RequestParam(required = false) CloudProvider provider,
            @Parameter(description = "Filter by recommendation type")
            @RequestParam(required = false) OptimizationRecommendationType type,
            @Parameter(description = "Filter by priority (CRITICAL, HIGH, MEDIUM, LOW)")
            @RequestParam(required = false) OptimizationPriority priority,
            @Parameter(description = "Filter by status (NEW, REVIEWED, APPROVED, IMPLEMENTED, REJECTED)")
            @RequestParam(required = false) OptimizationStatus status,
            @Parameter(description = "Minimum monthly savings threshold")
            @RequestParam(required = false) BigDecimal minSavings,
            @Parameter(description = "Search term for resource ID or type")
            @RequestParam(required = false) String search,
            @Parameter(description = "Page number (0-indexed, default 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default 20, max 100)")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort direction (asc, desc)")
            @RequestParam(defaultValue = "desc") String direction) {

        log.debug("GET /api/optimization/recommendations - provider: {}, type: {}, priority: {}, status: {}, page: {}",
                provider, type, priority, status, page);

        Sort sort = Sort.by("asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC, "estimatedMonthlySavings");
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), sort);

        Page<OptimizationRecommendationResponse> paged = optimizationService.getRecommendations(
                provider, type, priority, status, minSavings, search, pageable);

        return ResponseEntity.ok(ApiResponse.success(paged));
    }

    @GetMapping("/recommendations/{id}")
    @Operation(summary = "Get recommendation details", description = "Retrieve a single recommendation record with full utilization metrics")
    public ResponseEntity<ApiResponse<OptimizationRecommendationResponse>> getRecommendationById(
            @Parameter(description = "Recommendation ID") @PathVariable Long id) {
        log.debug("GET /api/optimization/recommendations/{}", id);
        OptimizationRecommendationResponse response = optimizationService.getRecommendationById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/recommendations/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Update recommendation status", description = "Transition recommendation status (REVIEWED, APPROVED, IMPLEMENTED, REJECTED) with implementation notes")
    public ResponseEntity<ApiResponse<OptimizationRecommendationResponse>> updateRecommendationStatus(
            @Parameter(description = "Recommendation ID") @PathVariable Long id,
            @Valid @RequestBody OptimizationStatusUpdateRequest request) {
        log.info("PATCH /api/optimization/recommendations/{}/status - target: {}", id, request.getStatus());
        OptimizationRecommendationResponse updated = optimizationService.updateStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @GetMapping("/recommendations/{id}/history")
    @Operation(summary = "Get recommendation audit history", description = "Retrieve chronological state transition log with operator details")
    public ResponseEntity<ApiResponse<List<OptimizationRecommendationHistoryResponse>>> getRecommendationHistory(
            @Parameter(description = "Recommendation ID") @PathVariable Long id) {
        log.debug("GET /api/optimization/recommendations/{}/history", id);
        List<OptimizationRecommendationHistoryResponse> history = optimizationService.getRecommendationHistory(id);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/savings-summary")
    @Operation(summary = "Get savings opportunity summary", description = "Retrieve breakdown of savings opportunity by provider, service, and recommendation category")
    public ResponseEntity<ApiResponse<SavingsSummaryResponse>> getSavingsSummary() {
        log.debug("GET /api/optimization/savings-summary");
        SavingsSummaryResponse summary = optimizationService.getSavingsSummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/resource-utilization")
    @Operation(summary = "Get resource utilization telemetry", description = "Retrieve monitored cloud resource CPU/memory telemetry snapshots and monitoring status")
    public ResponseEntity<ApiResponse<Page<ResourceUtilizationResponse>>> getResourceUtilization(
            @Parameter(description = "Page number (0-indexed, default 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default 20, max 100)")
            @RequestParam(defaultValue = "20") int size) {
        log.debug("GET /api/optimization/resource-utilization - page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
        Page<ResourceUtilizationResponse> paged = optimizationService.getResourceUtilization(pageable);
        return ResponseEntity.ok(ApiResponse.success(paged));
    }

    @GetMapping("/commitment-analysis")
    @Operation(summary = "Get commitment analysis", description = "Analyze baseline compute spending for Reserved Instances and Savings Plans eligibility")
    public ResponseEntity<ApiResponse<CommitmentAnalysisResponse>> getCommitmentAnalysis() {
        log.debug("GET /api/optimization/commitment-analysis");
        CommitmentAnalysisResponse response = optimizationService.getCommitmentAnalysis();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/analyze")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Run optimization analysis", description = "Trigger on-demand rightsizing, idle resource, and commitment analysis scan")
    public ResponseEntity<ApiResponse<Map<String, Object>>> runOptimizationAnalysis() {
        log.info("POST /api/optimization/analyze - manual trigger");
        Map<String, Object> result = optimizationService.runAnalysis();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/export")
    @Operation(summary = "Export optimization recommendations CSV", description = "Generate and download a CSV report of all optimization recommendations")
    public ResponseEntity<String> exportCsv() {
        log.info("GET /api/optimization/export - generating CSV");
        String csv = optimizationService.exportCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cost-optimization-recommendations.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}

