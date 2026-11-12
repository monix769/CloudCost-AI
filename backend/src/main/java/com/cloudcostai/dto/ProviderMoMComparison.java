package com.cloudcostai.dto;

import com.cloudcostai.entity.CloudProvider;
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
@Schema(description = "Provider-level month-over-month expenditure comparison")
public class ProviderMoMComparison {

    @Schema(description = "Cloud provider", example = "AWS")
    private CloudProvider provider;

    @Schema(description = "Current month expenditure", example = "12500.00")
    private BigDecimal currentMonthCost;

    @Schema(description = "Previous month expenditure", example = "11000.00")
    private BigDecimal previousMonthCost;

    @Schema(description = "Absolute change (current - previous)", example = "1500.00")
    private BigDecimal absoluteChange;

    @Schema(description = "Percentage change relative to previous month", example = "13.64")
    private BigDecimal percentageChange;
}

