package com.cloudcostai.controller;

import com.cloudcostai.analytics.forecast.CostForecastService;
import com.cloudcostai.analytics.forecast.ForecastHorizon;
import com.cloudcostai.analytics.forecast.ForecastMethod;
import com.cloudcostai.analytics.forecast.ForecastResult;
import com.cloudcostai.analytics.insight.FinOpsInsight;
import com.cloudcostai.analytics.insight.FinOpsInsightService;
import com.cloudcostai.dto.AccountCostResponse;
import com.cloudcostai.dto.AnalyticsOverviewResponse;
import com.cloudcostai.dto.ApiResponse;
import com.cloudcostai.dto.BudgetUtilizationResponse;
import com.cloudcostai.dto.CostAllocationDimension;
import com.cloudcostai.dto.CostAllocationResponse;
import com.cloudcostai.dto.MonthOverMonthResponse;
import com.cloudcostai.dto.ProviderCostResponse;
import com.cloudcostai.dto.ServiceCostResponse;
import com.cloudcostai.dto.SpendingTrendsSeriesResponse;
import com.cloudcostai.dto.TimeGranularity;
import com.cloudcostai.entity.CloudProvider;
import com.cloudcostai.service.AnalyticsExportService;
import com.cloudcostai.service.BudgetService;
import com.cloudcostai.service.CostAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "FinOps Analytics Engine", description = "Advanced multi-cloud FinOps analytics, cost forecasting, budget utilization, and rule-based insights")
public class FinOpsAnalyticsController {

    private final CostAnalyticsService costAnalyticsService;
    private final CostForecastService costForecastService;
    private final FinOpsInsightService finOpsInsightService;
    private final BudgetService budgetService;
    private final AnalyticsExportService analyticsExportService;

    @GetMapping("/overview")
    @Operation(summary = "Get FinOps Analytics Overview", description = "Aggregate high-level KPIs, month-over-month trajectory, budget health, and top cost drivers")
    public ResponseEntity<ApiResponse<AnalyticsOverviewResponse>> getOverview(
            @Parameter(description = "Optional currency filter (e.g. USD, EUR)")
            @RequestParam(required = false) String currency) {
        log.debug("GET /api/analytics/overview - currency: {}", currency);
        AnalyticsOverviewResponse overview = costAnalyticsService.getAnalyticsOverview(currency);
        overview.setActiveInsightsCount(finOpsInsightService.generateInsights(currency).size());
        return ResponseEntity.ok(ApiResponse.success(overview));
    }

    @GetMapping("/spending-trends")
    @Operation(summary = "Get Spending Trends", description = "Retrieve chronological spend data aggregated across DAILY, WEEKLY, MONTHLY, QUARTERLY, or YEARLY buckets")
    public ResponseEntity<ApiResponse<SpendingTrendsSeriesResponse>> getSpendingTrends(
            @Parameter(description = "Time granularity (DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY)")
            @RequestParam(required = false, defaultValue = "MONTHLY") TimeGranularity granularity,
            @Parameter(description = "Start date (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Optional currency filter")
            @RequestParam(required = false) String currency,
            @Parameter(description = "Optional cloud provider filter (AWS, AZURE, GCP)")
            @RequestParam(required = false) CloudProvider provider) {
        log.debug("GET /api/analytics/spending-trends - granularity: {}, start: {}, end: {}, currency: {}, provider: {}",
                granularity, startDate, endDate, currency, provider);
        SpendingTrendsSeriesResponse trends = costAnalyticsService.getSpendingTrends(granularity, startDate, endDate, currency, provider);
        return ResponseEntity.ok(ApiResponse.success(trends));
    }

    @GetMapping("/provider-comparison")
    @Operation(summary = "Get Provider Comparison", description = "Multi-cloud expenditure distribution, percentages, and record metrics across AWS, Azure, and GCP")
    public ResponseEntity<ApiResponse<List<ProviderCostResponse>>> getProviderComparison(
            @Parameter(description = "Start date (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Optional currency filter")
            @RequestParam(required = false) String currency) {
        log.debug("GET /api/analytics/provider-comparison - start: {}, end: {}, currency: {}", startDate, endDate, currency);
        List<ProviderCostResponse> providers = costAnalyticsService.getCostsByProvider(startDate, endDate, currency);
        return ResponseEntity.ok(ApiResponse.success(providers));
    }

    @GetMapping("/service-breakdown")
    @Operation(summary = "Get Service Breakdown", description = "Ranked expenditure breakdown by cloud service with percentage contribution")
    public ResponseEntity<ApiResponse<List<ServiceCostResponse>>> getServiceBreakdown(
            @Parameter(description = "Start date (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Maximum results to return")
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            @Parameter(description = "Optional currency filter")
            @RequestParam(required = false) String currency) {
        log.debug("GET /api/analytics/service-breakdown - limit: {}, currency: {}", limit, currency);
        List<ServiceCostResponse> services = costAnalyticsService.getCostsByService(startDate, endDate, limit, currency);
        return ResponseEntity.ok(ApiResponse.success(services));
    }

    @GetMapping("/top-spending-services")
    @Operation(summary = "Get Top Spending Cloud Services", description = "Retrieve top N most expensive cloud services")
    public ResponseEntity<ApiResponse<List<ServiceCostResponse>>> getTopSpendingServices(
            @Parameter(description = "Number of top services (default: 5)")
            @RequestParam(required = false, defaultValue = "5") Integer limit,
            @Parameter(description = "Optional currency filter")
            @RequestParam(required = false) String currency) {
        log.debug("GET /api/analytics/top-spending-services - limit: {}, currency: {}", limit, currency);
        List<ServiceCostResponse> topServices = costAnalyticsService.getTopSpendingServices(limit, currency);
        return ResponseEntity.ok(ApiResponse.success(topServices));
    }

    @GetMapping("/account-breakdown")
    @Operation(summary = "Get Cloud Account Breakdown", description = "Multi-cloud expenditure per connected cloud account")
    public ResponseEntity<ApiResponse<List<AccountCostResponse>>> getAccountBreakdown(
            @Parameter(description = "Start date (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Maximum results to return")
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            @Parameter(description = "Optional currency filter")
            @RequestParam(required = false) String currency) {
        log.debug("GET /api/analytics/account-breakdown - limit: {}, currency: {}", limit, currency);
        List<AccountCostResponse> accounts = costAnalyticsService.getCostsByAccount(startDate, endDate, limit, currency);
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @GetMapping("/top-spending-accounts")
    @Operation(summary = "Get Top Spending Accounts", description = "Retrieve top N highest expenditure cloud accounts")
    public ResponseEntity<ApiResponse<List<AccountCostResponse>>> getTopSpendingAccounts(
            @Parameter(description = "Number of top accounts (default: 5)")
            @RequestParam(required = false, defaultValue = "5") Integer limit,
            @Parameter(description = "Optional currency filter")
            @RequestParam(required = false) String currency) {
        log.debug("GET /api/analytics/top-spending-accounts - limit: {}, currency: {}", limit, currency);
        List<AccountCostResponse> topAccounts = costAnalyticsService.getTopSpendingAccounts(limit, currency);
        return ResponseEntity.ok(ApiResponse.success(topAccounts));
    }

    @GetMapping("/month-over-month")
    @Operation(summary = "Get Month-over-Month Comparison", description = "Financial comparison between current month and previous month including provider and service deltas")
    public ResponseEntity<ApiResponse<MonthOverMonthResponse>> getMonthOverMonth(
            @Parameter(description = "Optional currency filter")
            @RequestParam(required = false) String currency) {
        log.debug("GET /api/analytics/month-over-month - currency: {}", currency);
        MonthOverMonthResponse mom = costAnalyticsService.getMonthOverMonthComparison(currency);
        return ResponseEntity.ok(ApiResponse.success(mom));
    }

    @GetMapping("/forecast")
    @Operation(summary = "Generate Statistical Cost Forecast", description = "Run Java time-series forecast (Moving Average or Exponential Smoothing) for 7, 30, or 90 days")
    public ResponseEntity<ApiResponse<ForecastResult>> getForecast(
            @Parameter(description = "Forecast horizon (e.g. 7D, 30D, 90D)")
            @RequestParam(required = false, defaultValue = "30D") String horizon,
            @Parameter(description = "Forecasting method (MOVING_AVERAGE, EXPONENTIAL_SMOOTHING)")
            @RequestParam(required = false, defaultValue = "EXPONENTIAL_SMOOTHING") ForecastMethod method,
            @Parameter(description = "Alpha smoothing factor for exponential smoothing (0.0 to 1.0, default 0.3)")
            @RequestParam(required = false) Double alpha,
            @Parameter(description = "Rolling window day size for moving average (default 14)")
            @RequestParam(required = false) Integer windowSize,
            @Parameter(description = "Optional currency filter")
            @RequestParam(required = false) String currency) {
        log.debug("GET /api/analytics/forecast - horizon: {}, method: {}, alpha: {}, window: {}, currency: {}",
                horizon, method, alpha, windowSize, currency);
        ForecastHorizon forecastHorizon = ForecastHorizon.fromString(horizon);
        ForecastResult result = costForecastService.generateForecast(forecastHorizon, method, alpha, windowSize, currency);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/budget-utilization")
    @Operation(summary = "Get Budget Utilization Analytics", description = "Evaluate consumption, burn velocity, run rate, and overspending risks across active budgets")
    public ResponseEntity<ApiResponse<BudgetUtilizationResponse>> getBudgetUtilization(
            @Parameter(description = "Optional currency filter")
            @RequestParam(required = false) String currency) {
        log.debug("GET /api/analytics/budget-utilization - currency: {}", currency);
        BudgetUtilizationResponse budgetAnalytics = budgetService.getBudgetUtilizationAnalytics(currency);
        return ResponseEntity.ok(ApiResponse.success(budgetAnalytics));
    }

    @GetMapping("/cost-allocation")
    @Operation(summary = "Get Multi-Dimensional Cost Allocation", description = "Allocate spend by PROVIDER, ACCOUNT, SERVICE, REGION, or TAG")
    public ResponseEntity<ApiResponse<CostAllocationResponse>> getCostAllocation(
            @Parameter(description = "Allocation dimension (PROVIDER, ACCOUNT, SERVICE, REGION, TAG)")
            @RequestParam(required = false, defaultValue = "SERVICE") CostAllocationDimension dimension,
            @Parameter(description = "Start date (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Optional currency filter")
            @RequestParam(required = false) String currency) {
        log.debug("GET /api/analytics/cost-allocation - dimension: {}, start: {}, end: {}, currency: {}",
                dimension, startDate, endDate, currency);
        CostAllocationResponse allocation = costAnalyticsService.getCostAllocation(dimension, startDate, endDate, currency);
        return ResponseEntity.ok(ApiResponse.success(allocation));
    }

    @GetMapping("/insights")
    @Operation(summary = "Get Actionable FinOps Insights", description = "Evaluate rule engine against live billing data to produce prioritized savings and governance insights")
    public ResponseEntity<ApiResponse<List<FinOpsInsight>>> getInsights(
            @Parameter(description = "Optional currency filter")
            @RequestParam(required = false) String currency) {
        log.debug("GET /api/analytics/insights - currency: {}", currency);
        List<FinOpsInsight> insights = finOpsInsightService.generateInsights(currency);
        return ResponseEntity.ok(ApiResponse.success(insights));
    }

    // --- CSV Export Endpoints (Requirement 15) ---

    @GetMapping("/export/spending-trends")
    @Operation(summary = "Export Spending Trends as CSV", description = "Download CSV of trend series data")
    public ResponseEntity<byte[]> exportSpendingTrends(
            @RequestParam(required = false, defaultValue = "MONTHLY") TimeGranularity granularity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) CloudProvider provider) {
        byte[] csvBytes = analyticsExportService.exportSpendingTrendsCsv(granularity, startDate, endDate, currency, provider);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"spending-trends.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvBytes);
    }

    @GetMapping("/export/provider-comparison")
    @Operation(summary = "Export Provider Comparison as CSV", description = "Download CSV of multi-cloud provider distribution")
    public ResponseEntity<byte[]> exportProviderComparison(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String currency) {
        byte[] csvBytes = analyticsExportService.exportProviderComparisonCsv(startDate, endDate, currency);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"provider-comparison.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvBytes);
    }

    @GetMapping("/export/service-breakdown")
    @Operation(summary = "Export Service Breakdown as CSV", description = "Download CSV of service cost breakdown")
    public ResponseEntity<byte[]> exportServiceBreakdown(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "50") Integer limit,
            @RequestParam(required = false) String currency) {
        byte[] csvBytes = analyticsExportService.exportServiceBreakdownCsv(startDate, endDate, limit, currency);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"service-breakdown.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvBytes);
    }

    @GetMapping("/export/forecast")
    @Operation(summary = "Export Forecast Projections as CSV", description = "Download CSV of forecast series with confidence intervals")
    public ResponseEntity<byte[]> exportForecast(
            @RequestParam(required = false, defaultValue = "30D") String horizon,
            @RequestParam(required = false, defaultValue = "EXPONENTIAL_SMOOTHING") ForecastMethod method,
            @RequestParam(required = false) Double alpha,
            @RequestParam(required = false) Integer windowSize,
            @RequestParam(required = false) String currency) {
        ForecastHorizon forecastHorizon = ForecastHorizon.fromString(horizon);
        byte[] csvBytes = analyticsExportService.exportForecastCsv(forecastHorizon, method, alpha, windowSize, currency);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cost-forecast.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvBytes);
    }

    @GetMapping("/export/insights")
    @Operation(summary = "Export FinOps Insights as CSV", description = "Download CSV of active actionable insights and recommendations")
    public ResponseEntity<byte[]> exportInsights(
            @RequestParam(required = false) String currency) {
        byte[] csvBytes = analyticsExportService.exportInsightsCsv(currency);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"finops-insights.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvBytes);
    }
}

