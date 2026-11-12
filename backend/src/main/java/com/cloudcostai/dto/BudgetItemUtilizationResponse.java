package com.cloudcostai.dto;

import com.cloudcostai.entity.BudgetPeriod;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detailed utilization analytics for a specific budget definition")
public class BudgetItemUtilizationResponse {

    @Schema(description = "Budget record ID", example = "1")
    private Long budgetId;

    @Schema(description = "Budget name", example = "Production Multi-Cloud Q4")
    private String budgetName;

    @Schema(description = "Allocated spend limit", example = "50000.00")
    private BigDecimal amountLimit;

    @Schema(description = "Current live consumption", example = "38500.00")
    private BigDecimal currentSpend;

    @Schema(description = "Remaining available budget", example = "11500.00")
    private BigDecimal remainingBudget;

    @Schema(description = "Current percentage utilized", example = "77.00")
    private BigDecimal utilizationPercentage;

    @Schema(description = "Average spend velocity per day", example = "1283.33")
    private BigDecimal burnRatePerDay;

    @Schema(description = "Projected total consumption by end of period based on current run rate", example = "48500.00")
    private BigDecimal estimatedEndOfPeriodSpend;

    @Schema(description = "Overspending risk level (LOW, MEDIUM, HIGH, CRITICAL)", example = "MEDIUM")
    private String overspendingRisk;

    @Schema(description = "FinOps health status classification", example = "WARNING")
    private BudgetHealthStatus healthStatus;

    @Schema(description = "Budget cadence period", example = "MONTHLY")
    private BudgetPeriod period;

    @Schema(description = "Alert threshold percentage", example = "80.00")
    private BigDecimal alertThresholdPercentage;
}

