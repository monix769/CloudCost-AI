package com.cloudcostai.dto;

import com.cloudcostai.entity.BudgetPeriod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload to configure a financial budget limit")
public class BudgetRequest {

    @NotBlank(message = "Budget name is required")
    @Size(max = 150, message = "Budget name cannot exceed 150 characters")
    @Schema(description = "Department, project or workload budget title", example = "Q4 Engineering Cloud Budget")
    private String name;

    @NotNull(message = "Budget amount limit is required")
    @DecimalMin(value = "0.01", message = "Budget amount limit must be greater than zero")
    @Schema(description = "Total expenditure cap", example = "50000.00")
    private BigDecimal amountLimit;

    @Size(max = 10, message = "Currency code cannot exceed 10 characters")
    @Schema(description = "Currency ISO code", example = "USD")
    @Builder.Default
    private String currency = "USD";

    @Schema(description = "Cadence for budget tracking (MONTHLY, QUARTERLY, ANNUAL)", example = "MONTHLY")
    @Builder.Default
    private BudgetPeriod period = BudgetPeriod.MONTHLY;

    @NotNull(message = "Alert threshold percentage is required")
    @DecimalMin(value = "1.00", message = "Alert threshold must be at least 1.00%")
    @DecimalMax(value = "100.00", message = "Alert threshold cannot exceed 100.00%")
    @Schema(description = "Percentage of budget consumed that triggers warning alerts", example = "80.00")
    @Builder.Default
    private BigDecimal alertThresholdPercentage = new BigDecimal("80.00");

    @Size(max = 50, message = "Status cannot exceed 50 characters")
    @Schema(description = "Budget operational status", example = "ACTIVE")
    @Builder.Default
    private String status = "ACTIVE";
}
