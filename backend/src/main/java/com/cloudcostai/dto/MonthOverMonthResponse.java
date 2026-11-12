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
@Schema(description = "Financial month-over-month comparative analysis report")
public class MonthOverMonthResponse {

    @Schema(description = "Current month period key", example = "2026-11")
    private String currentMonth;

    @Schema(description = "Previous month period key", example = "2026-10")
    private String previousMonth;

    @Schema(description = "Expenditure in current month", example = "24500.00")
    private BigDecimal currentMonthCost;

    @Schema(description = "Expenditure in previous month", example = "21800.00")
    private BigDecimal previousMonthCost;

    @Schema(description = "Absolute difference (currentMonth - previousMonth)", example = "2700.00")
    private BigDecimal absoluteChange;

    @Schema(description = "Percentage difference relative to previous month", example = "12.39")
    private BigDecimal percentageChange;

    @Schema(description = "Direction of change: INCREASE, DECREASE, or NO_CHANGE", example = "INCREASE")
    private String direction;

    @Schema(description = "Monetary currency", example = "USD")
    @Builder.Default
    private String currency = "USD";

    @Schema(description = "Provider-by-provider comparison")
    @Builder.Default
    private List<ProviderMoMComparison> providerBreakdown = new ArrayList<>();

    @Schema(description = "Top spending service changes month-over-month")
    @Builder.Default
    private List<ServiceMoMComparison> topServiceChanges = new ArrayList<>();

    @Schema(description = "Human-readable explainable FinOps observations", example = "[\"Cloud expenditure increased by 12.39%.\", \"AWS expenditure contributed the largest increase of $1,800.00.\"]")
    @Builder.Default
    private List<String> insights = new ArrayList<>();
}

