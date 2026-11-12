package com.cloudcostai.analytics.forecast;

import java.util.List;

public interface Forecaster {

    ForecastMethod getMethod();

    ForecastResult forecast(
            List<HistoricalDataPoint> historicalPoints,
            ForecastHorizon horizon,
            String currency,
            Double alpha,
            Integer windowSize);
}

