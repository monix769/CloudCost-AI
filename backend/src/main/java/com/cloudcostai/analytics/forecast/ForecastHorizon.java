package com.cloudcostai.analytics.forecast;

import lombok.Getter;

@Getter
public enum ForecastHorizon {
    DAYS_7(7, "7 Days"),
    DAYS_30(30, "30 Days"),
    DAYS_90(90, "90 Days");

    private final int days;
    private final String label;

    ForecastHorizon(int days, String label) {
        this.days = days;
        this.label = label;
    }

    public static ForecastHorizon fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return DAYS_30;
        }
        String clean = text.trim().toUpperCase();
        return switch (clean) {
            case "7", "7D", "7_DAYS", "DAYS_7", "SEVEN_DAYS" -> DAYS_7;
            case "90", "90D", "90_DAYS", "DAYS_90", "NINETY_DAYS" -> DAYS_90;
            default -> DAYS_30;
        };
    }
}

