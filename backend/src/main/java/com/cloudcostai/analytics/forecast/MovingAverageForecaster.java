package com.cloudcostai.analytics.forecast;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class MovingAverageForecaster implements Forecaster {

    private static final int DEFAULT_WINDOW_SIZE = 14;

    @Override
    public ForecastMethod getMethod() {
        return ForecastMethod.MOVING_AVERAGE;
    }

    @Override
    public ForecastResult forecast(
            List<HistoricalDataPoint> historicalPoints,
            ForecastHorizon horizon,
            String currency,
            Double alpha,
            Integer windowSize) {

        int horizonDays = (horizon != null) ? horizon.getDays() : 30;
        String horizonLabel = (horizon != null) ? horizon.getLabel() : "30 Days";
        int window = (windowSize != null && windowSize > 0) ? windowSize : DEFAULT_WINDOW_SIZE;

        if (historicalPoints == null || historicalPoints.isEmpty()) {
            return ForecastResult.builder()
                    .method(getMethod())
                    .horizon(horizonLabel)
                    .currency(currency)
                    .totalHistoricalCost(BigDecimal.ZERO)
                    .estimatedTotalForecastCost(BigDecimal.ZERO)
                    .averageDailyForecastCost(BigDecimal.ZERO)
                    .confidenceLevel("N/A - Insufficient historical data")
                    .windowSize(window)
                    .statusMessage("Insufficient historical data available to compute moving average forecast.")
                    .limitations("Requires at least 2 historical days of billing data for statistical projection.")
                    .historicalData(new ArrayList<>())
                    .forecastData(new ArrayList<>())
                    .build();
        }

        BigDecimal totalHistorical = historicalPoints.stream()
                .map(HistoricalDataPoint::getCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        int n = historicalPoints.size();
        int effectiveWindow = Math.min(window, n);
        List<HistoricalDataPoint> windowSubList = historicalPoints.subList(n - effectiveWindow, n);

        BigDecimal sumWindow = windowSubList.stream()
                .map(HistoricalDataPoint::getCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageDailyForecast = sumWindow.divide(BigDecimal.valueOf(effectiveWindow), 4, RoundingMode.HALF_UP);

        // Compute sample standard deviation for the window
        double mean = averageDailyForecast.doubleValue();
        double varianceSum = 0.0;
        for (HistoricalDataPoint pt : windowSubList) {
            double diff = pt.getCost().doubleValue() - mean;
            varianceSum += diff * diff;
        }

        double stdDev = (effectiveWindow > 1) ? Math.sqrt(varianceSum / (effectiveWindow - 1)) : 0.0;
        double marginOfError = 1.96 * stdDev; // 95% Confidence Interval for normal residual distribution

        BigDecimal lowerDaily = BigDecimal.valueOf(Math.max(0.0, mean - marginOfError)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal upperDaily = BigDecimal.valueOf(mean + marginOfError).setScale(2, RoundingMode.HALF_UP);
        BigDecimal forecastPerDay = averageDailyForecast.setScale(2, RoundingMode.HALF_UP);

        LocalDate lastHistoricalDate = historicalPoints.get(n - 1).getDate();
        if (lastHistoricalDate == null) {
            lastHistoricalDate = LocalDate.now();
        }

        List<ForecastDataPoint> forecastPoints = new ArrayList<>(horizonDays);
        for (int i = 1; i <= horizonDays; i++) {
            forecastPoints.add(ForecastDataPoint.builder()
                    .date(lastHistoricalDate.plusDays(i))
                    .forecastedCost(forecastPerDay)
                    .lowerBound(lowerDaily)
                    .upperBound(upperDaily)
                    .estimate(true)
                    .build());
        }

        BigDecimal totalForecast = forecastPerDay.multiply(BigDecimal.valueOf(horizonDays)).setScale(2, RoundingMode.HALF_UP);

        String confidenceDesc = (effectiveWindow > 1)
                ? String.format("95%% Confidence Interval (±1.96 * standard error: $%.2f)", marginOfError)
                : "Point Estimate (insufficient sample variance for interval bounds)";

        String limitations = "Moving average assumes stationary variance and constant linear consumption. " +
                "Does not account for non-linear seasonal spikes, unexpected architectural expansions, or spot-instance fluctuations.";

        return ForecastResult.builder()
                .method(getMethod())
                .horizon(horizonLabel)
                .currency(currency)
                .totalHistoricalCost(totalHistorical)
                .estimatedTotalForecastCost(totalForecast)
                .averageDailyForecastCost(forecastPerDay)
                .confidenceLevel(confidenceDesc)
                .windowSize(effectiveWindow)
                .statusMessage(String.format("Generated %d-day moving average forecast based on last %d days of billing history.", horizonDays, effectiveWindow))
                .limitations(limitations)
                .historicalData(historicalPoints)
                .forecastData(forecastPoints)
                .build();
    }
}

