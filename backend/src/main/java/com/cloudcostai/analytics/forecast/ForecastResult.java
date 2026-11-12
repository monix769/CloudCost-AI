package com.cloudcostai.analytics.forecast;

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
@Schema(description = "Statistical cost forecast outcome and projection model results")
public class ForecastResult {

    @Schema(description = "Forecasting model methodology employed", example = "EXPONENTIAL_SMOOTHING")
    private ForecastMethod method;

    @Schema(description = "Forecasting horizon timeframe", example = "30 Days")
    private String horizon;

    @Schema(description = "Monetary currency unit", example = "USD")
    private String currency;

    @Schema(description = "Total historical expenditure across training period", example = "38450.00")
    private BigDecimal totalHistoricalCost;

    @Schema(description = "Estimated cumulative expenditure across forecast horizon", example = "41200.50")
    private BigDecimal estimatedTotalForecastCost;

    @Schema(description = "Mean projected daily expenditure", example = "1373.35")
    private BigDecimal averageDailyForecastCost;

    @Schema(description = "Statistical confidence or uncertainty level explanation", example = "95% Confidence Interval (±1.96 * Residual Standard Error)")
    private String confidenceLevel;

    @Schema(description = "Alpha smoothing parameter utilized for exponential smoothing", example = "0.3")
    private Double alpha;

    @Schema(description = "Rolling window day size utilized for moving average", example = "14")
    private Integer windowSize;

    @Schema(description = "Descriptive summary of data sufficiency and baseline parameters")
    private String statusMessage;

    @Schema(description = "Methodological constraints, assumptions, and statistical limitations")
    private String limitations;

    @Schema(description = "Chronological historical data points utilized for model training")
    @Builder.Default
    private List<HistoricalDataPoint> historicalData = new ArrayList<>();

    @Schema(description = "Future projected daily data points with confidence bands")
    @Builder.Default
    private List<ForecastDataPoint> forecastData = new ArrayList<>();
}

