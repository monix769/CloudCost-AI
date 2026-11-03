package com.cloudcostai.controller;

import com.cloudcostai.dto.ApiResponse;
import com.cloudcostai.dto.CostSummaryResponse;
import com.cloudcostai.dto.CostTrendResponse;
import com.cloudcostai.dto.ProviderCostResponse;
import com.cloudcostai.dto.ServiceCostResponse;
import com.cloudcostai.service.CostAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/costs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cost Analytics", description = "FinOps analytics, spend aggregations, provider breakdowns, and historical trends")
public class CostAnalyticsController {

    private final CostAnalyticsService costAnalyticsService;

    @GetMapping("/summary")
    @Operation(summary = "Get overall cost summary", description = "Calculate total expenditure, average daily spend, and total record count within a date window")
    public ResponseEntity<ApiResponse<CostSummaryResponse>> getCostSummary(
            @Parameter(description = "Start date (inclusive, YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (inclusive, YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.debug("GET /api/costs/summary - startDate: {}, endDate: {}", startDate, endDate);
        CostSummaryResponse summary = costAnalyticsService.getCostSummary(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/by-provider")
    @Operation(summary = "Get costs by cloud provider", description = "Calculate aggregate spend, percentage distribution, and record counts for AWS, Azure, and GCP")
    public ResponseEntity<ApiResponse<List<ProviderCostResponse>>> getCostsByProvider(
            @Parameter(description = "Start date (inclusive, YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (inclusive, YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.debug("GET /api/costs/by-provider - startDate: {}, endDate: {}", startDate, endDate);
        List<ProviderCostResponse> providerCosts = costAnalyticsService.getCostsByProvider(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(providerCosts));
    }

    @GetMapping("/by-service")
    @Operation(summary = "Get costs by cloud service", description = "Calculate aggregate spend and percentage share grouped by cloud service, ranked by expenditure")
    public ResponseEntity<ApiResponse<List<ServiceCostResponse>>> getCostsByService(
            @Parameter(description = "Start date (inclusive, YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (inclusive, YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Max number of top services to return (e.g. 10)")
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        log.debug("GET /api/costs/by-service - startDate: {}, endDate: {}, limit: {}", startDate, endDate, limit);
        List<ServiceCostResponse> serviceCosts = costAnalyticsService.getCostsByService(startDate, endDate, limit);
        return ResponseEntity.ok(ApiResponse.success(serviceCosts));
    }

    @GetMapping("/trends")
    @Operation(summary = "Get monthly spend trends", description = "Retrieve chronological monthly cost time-series with per-provider spend distribution")
    public ResponseEntity<ApiResponse<List<CostTrendResponse>>> getCostTrends(
            @Parameter(description = "Start date (inclusive, YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (inclusive, YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.debug("GET /api/costs/trends - startDate: {}, endDate: {}", startDate, endDate);
        List<CostTrendResponse> trends = costAnalyticsService.getCostTrends(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(trends));
    }
}
