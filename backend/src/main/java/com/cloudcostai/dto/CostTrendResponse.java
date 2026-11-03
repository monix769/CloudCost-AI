package com.cloudcostai.dto;

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
@Schema(description = "Chronological cloud spend data point for trend analytics")
public class CostTrendResponse {

    @Schema(description = "Formatted period identifier (YYYY-MM)", example = "2026-05")
    private String period;

    @Schema(description = "Calendar Year", example = "2026")
    private Integer year;

    @Schema(description = "Calendar Month (1-12)", example = "5")
    private Integer month;

    @Schema(description = "Total expenditure across all providers in this month", example = "8450.25")
    private BigDecimal totalCost;

    @Schema(description = "AWS expenditure for this month", example = "4200.00")
    @Builder.Default
    private BigDecimal awsCost = BigDecimal.ZERO;

    @Schema(description = "Azure expenditure for this month", example = "2150.25")
    @Builder.Default
    private BigDecimal azureCost = BigDecimal.ZERO;

    @Schema(description = "GCP expenditure for this month", example = "2100.00")
    @Builder.Default
    private BigDecimal gcpCost = BigDecimal.ZERO;
}
