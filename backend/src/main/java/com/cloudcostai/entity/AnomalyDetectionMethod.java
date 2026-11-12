package com.cloudcostai.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AnomalyDetectionMethod {
    Z_SCORE("Z_SCORE", "Z-Score Statistical Outlier Detection"),
    MOVING_AVERAGE("MOVING_AVERAGE", "Moving Average Percentage Deviation"),
    SPENDING_SPIKE("SPENDING_SPIKE", "Sudden Daily Spend Spike Detection"),
    BUDGET_BASED("BUDGET_BASED", "Budget Threshold Breach Detection");

    private final String code;
    private final String displayName;

    AnomalyDetectionMethod(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static AnomalyDetectionMethod fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Z_SCORE;
        }
        String clean = text.trim();
        for (AnomalyDetectionMethod m : AnomalyDetectionMethod.values()) {
            if (m.name().equalsIgnoreCase(clean) || m.code.equalsIgnoreCase(clean)) {
                return m;
            }
        }
        throw new IllegalArgumentException("Unknown anomaly detection method: '" + text + "'. Supported: Z_SCORE, MOVING_AVERAGE, SPENDING_SPIKE, BUDGET_BASED");
    }
}

