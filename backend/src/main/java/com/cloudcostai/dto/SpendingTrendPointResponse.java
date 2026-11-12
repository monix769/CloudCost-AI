package com.cloudcostai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Individual aggregated spend data point for flexible trend analysis")
public class SpendingTrendPointResponse {

    @Schema(description = "Period label (e.g. '2026-11-01', '2026-W45', '2026-11', '2026-Q4')", example = "2026-11-01")
    private String period;

    @Schema(description = "Period start boundary")
    private LocalDate startDate;

    @Schema(description = "Period end boundary")
    private LocalDate endDate;

    @Schema(description = "Total expenditure within this discrete period bucket", example = "1245.50")
    private BigDecimal totalCost;

    @Schema(description = "Average daily expenditure within this period", example = "177.92")
    private BigDecimal averageDailyCost;

    @Schema(description = "Minimum daily expenditure within this period", example = "110.00")
    private BigDecimal minCost;

    @Schema(description = "Maximum daily expenditure within this period", example = "240.50")
    private BigDecimal maxCost;

    @Schema(description = "Percentage difference compared to preceding period in series", example = "4.25")
    private BigDecimal percentageChange;

    @Schema(description = "Total expenditure of preceding period", example = "1195.00")
    private BigDecimal previousPeriodCost;

    @Schema(description = "AWS expenditure contribution", example = "600.00")
    @Builder.Default
    private BigDecimal awsCost = BigDecimal.ZERO;

    @Schema(description = "Azure expenditure contribution", example = "345.50")
    @Builder.Default
    private BigDecimal azureCost = BigDecimal.ZERO;

    @Schema(description = "GCP expenditure contribution", example = "300.00")
    @Builder.Default
    private BigDecimal gcpCost = BigDecimal.ZERO;

    @Schema(description = "Number of underlying billing records aggregated", example = "42")
    private Long recordCount;
}

