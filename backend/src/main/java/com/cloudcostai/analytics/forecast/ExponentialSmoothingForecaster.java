package com.cloudcostai.analytics.forecast;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class ExponentialSmoothingForecaster implements Forecaster {

    private static final double DEFAULT_ALPHA = 0.3;

    @Override
    public ForecastMethod getMethod() {
        return ForecastMethod.EXPONENTIAL_SMOOTHING;
    }

    @Override
    public ForecastResult forecast(
            List<HistoricalDataPoint> historicalPoints,
            ForecastHorizon horizon,
            String currency,
            Double alphaParam,
            Integer windowSize) {

        int horizonDays = (horizon != null) ? horizon.getDays() : 30;
        String horizonLabel = (horizon != null) ? horizon.getLabel() : "30 Days";
        double alpha = (alphaParam != null && alphaParam > 0.0 && alphaParam <= 1.0) ? alphaParam : DEFAULT_ALPHA;

        if (historicalPoints == null || historicalPoints.isEmpty()) {
            return ForecastResult.builder()
                    .method(getMethod())
                    .horizon(horizonLabel)
                    .currency(currency)
                    .totalHistoricalCost(BigDecimal.ZERO)
                    .estimatedTotalForecastCost(BigDecimal.ZERO)
                    .averageDailyForecastCost(BigDecimal.ZERO)
                    .confidenceLevel("N/A - Insufficient historical data")
                    .alpha(alpha)
                    .statusMessage("Insufficient historical data available to compute exponential smoothing forecast.")
                    .limitations("Requires at least 2 historical daily billing records.")
                    .historicalData(new ArrayList<>())
                    .forecastData(new ArrayList<>())
                    .build();
        }

        BigDecimal totalHistorical = historicalPoints.stream()
                .map(HistoricalDataPoint::getCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        int n = historicalPoints.size();

        // Single Exponential Smoothing (SES)
        // Level: L_0 = y_0
        // L_t = alpha * y_t + (1 - alpha) * L_{t-1}
        double level = historicalPoints.get(0).getCost().doubleValue();
        double sumSquaredResiduals = 0.0;
        int residualCount = 0;

        for (int t = 1; t < n; t++) {
            double actual = historicalPoints.get(t).getCost().doubleValue();
            double predicted = level;
            double error = actual - predicted;
            sumSquaredResiduals += error * error;
            residualCount++;

            level = (alpha * actual) + ((1.0 - alpha) * level);
        }

        double residualStdError = (residualCount > 0)
                ? Math.sqrt(sumSquaredResiduals / residualCount)
                : 0.0;

        double baseForecast = Math.max(0.0, level);
        BigDecimal averageDailyForecast = BigDecimal.valueOf(baseForecast).setScale(2, RoundingMode.HALF_UP);

        LocalDate lastHistoricalDate = historicalPoints.get(n - 1).getDate();
        if (lastHistoricalDate == null) {
            lastHistoricalDate = LocalDate.now();
        }

        List<ForecastDataPoint> forecastPoints = new ArrayList<>(horizonDays);
        BigDecimal cumulativeForecast = BigDecimal.ZERO;

        for (int h = 1; h <= horizonDays; h++) {
            // Variance for h-step ahead forecast in SES: Var(e_h) = sigma_e^2 * (1 + (h - 1) * alpha^2)
            double horizonStdError = residualStdError * Math.sqrt(1.0 + (h - 1) * (alpha * alpha));
            double marginOfError = 1.96 * horizonStdError; // 95% Confidence interval

            BigDecimal lower = BigDecimal.valueOf(Math.max(0.0, baseForecast - marginOfError)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal upper = BigDecimal.valueOf(baseForecast + marginOfError).setScale(2, RoundingMode.HALF_UP);

            forecastPoints.add(ForecastDataPoint.builder()
                    .date(lastHistoricalDate.plusDays(h))
                    .forecastedCost(averageDailyForecast)
                    .lowerBound(lower)
                    .upperBound(upper)
                    .estimate(true)
                    .build());

            cumulativeForecast = cumulativeForecast.add(averageDailyForecast);
        }

        String confidenceDesc = (residualCount > 0)
                ? String.format("95%% Confidence Interval expanding over horizon (alpha=%.2f, initial standard error: $%.2f)", alpha, residualStdError)
                : "Point Estimate (single data point baseline)";

        String limitations = "Single exponential smoothing is optimal for series without pronounced trend or seasonality. " +
                "Confidence bands widen as the projection horizon extends into the future (up to " + horizonLabel + ").";

        return ForecastResult.builder()
                .method(getMethod())
                .horizon(horizonLabel)
                .currency(currency)
                .totalHistoricalCost(totalHistorical)
                .estimatedTotalForecastCost(cumulativeForecast.setScale(2, RoundingMode.HALF_UP))
                .averageDailyForecastCost(averageDailyForecast)
                .confidenceLevel(confidenceDesc)
                .alpha(alpha)
                .statusMessage(String.format("Generated %d-day Simple Exponential Smoothing forecast with alpha=%.2f based on %d days of billing data.", horizonDays, alpha, n))
                .limitations(limitations)
                .historicalData(historicalPoints)
                .forecastData(forecastPoints)
                .build();
    }
}

