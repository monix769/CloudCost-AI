package com.cloudcostai.analytics.forecast;

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
@Schema(description = "Statistical projected cost data point")
public class ForecastDataPoint {

    @Schema(description = "Projected date", example = "2026-11-20")
    private LocalDate date;

    @Schema(description = "Estimated cost point projection", example = "1320.75")
    private BigDecimal forecastedCost;

    @Schema(description = "Lower boundary of statistical confidence interval (95%)", example = "1180.20")
    private BigDecimal lowerBound;

    @Schema(description = "Upper boundary of statistical confidence interval (95%)", example = "1461.30")
    private BigDecimal upperBound;

    @Schema(description = "Flag indicating this is an estimate", example = "true")
    @Builder.Default
    private boolean estimate = true;
}

