package com.cloudcostai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Holistic FinOps executive dashboard overview analytics")
public class AnalyticsOverviewResponse {

    @Schema(description = "Primary cost summary metrics")
    private CostSummaryResponse summary;

    @Schema(description = "Month-over-month financial comparison")
    private MonthOverMonthResponse monthOverMonth;

    @Schema(description = "Multi-cloud provider cost distribution")
    @Builder.Default
    private List<ProviderCostResponse> providerDistribution = new ArrayList<>();

    @Schema(description = "Top spending cloud services")
    @Builder.Default
    private List<ServiceCostResponse> topServices = new ArrayList<>();

    @Schema(description = "Top spending cloud accounts")
    @Builder.Default
    private List<AccountCostResponse> topAccounts = new ArrayList<>();

    @Schema(description = "Organizational budget utilization overview")
    private BudgetUtilizationResponse budgetHealth;

    @Schema(description = "Number of active actionable FinOps insights", example = "4")
    private Integer activeInsightsCount;

    @Schema(description = "Monetary currency unit", example = "USD")
    @Builder.Default
    private String currency = "USD";
}

