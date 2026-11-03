package com.cloudcostai.dto;

import com.cloudcostai.entity.BudgetPeriod;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Details and live utilization metrics of a budget")
public class BudgetResponse {

    @Schema(description = "Budget record ID", example = "1")
    private Long id;

    @Schema(description = "Organization ID", example = "1")
    private Long organizationId;

    @Schema(description = "Budget name", example = "Q4 Engineering Cloud Budget")
    private String name;

    @Schema(description = "Configured financial ceiling", example = "50000.00")
    private BigDecimal amountLimit;

    @Schema(description = "Aggregated live spend within current period", example = "34210.50")
    private BigDecimal currentSpend;

    @Schema(description = "Live percentage of budget limit consumed", example = "68.42")
    private BigDecimal utilizationPercentage;

    @Schema(description = "Remaining monetary buffer before limit breached", example = "15789.50")
    private BigDecimal remainingAmount;

    @Schema(description = "Monetary currency", example = "USD")
    private String currency;

    @Schema(description = "Budget tracking period", example = "MONTHLY")
    private BudgetPeriod period;

    @Schema(description = "Configured alert warning threshold", example = "80.00")
    private BigDecimal alertThresholdPercentage;

    @Schema(description = "Operational status", example = "ACTIVE")
    private String status;

    @Schema(description = "Flag indicating if spending has exceeded the budget cap", example = "false")
    private boolean isExceeded;

    @Schema(description = "Flag indicating if spending is at or above the alert threshold", example = "false")
    private boolean isNearThreshold;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Last update timestamp")
    private Instant updatedAt;
}
