package com.cloudcostai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Overall organizational budget utilization analytics and forecasting report")
public class BudgetUtilizationResponse {

    @Schema(description = "Aggregate allocated budget across all active targets", example = "100000.00")
    private BigDecimal totalAllocatedBudget;

    @Schema(description = "Total live spend against active budgets", example = "78500.00")
    private BigDecimal totalCurrentSpend;

    @Schema(description = "Total aggregate remaining budget", example = "21500.00")
    private BigDecimal totalRemainingBudget;

    @Schema(description = "Overall aggregate utilization percentage", example = "78.50")
    private BigDecimal overallUtilizationPercentage;

    @Schema(description = "Projected aggregate spend by period close", example = "96000.00")
    private BigDecimal estimatedEndOfPeriodSpend;

    @Schema(description = "Estimated total overrun amount beyond allocated limit if applicable", example = "0.00")
    private BigDecimal projectedOverrun;

    @Schema(description = "Overall organizational budget health status", example = "WARNING")
    private BudgetHealthStatus overallHealthStatus;

    @Schema(description = "Monetary currency unit", example = "USD")
    @Builder.Default
    private String currency = "USD";

    @Schema(description = "Breakdown per individual budget definition")
    @Builder.Default
    private List<BudgetItemUtilizationResponse> budgetBreakdowns = new ArrayList<>();
}

