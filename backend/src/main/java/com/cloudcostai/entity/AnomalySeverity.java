package com.cloudcostai.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AnomalySeverity {
    LOW("LOW"),
    MEDIUM("MEDIUM"),
    HIGH("HIGH"),
    CRITICAL("CRITICAL");

    private final String value;

    AnomalySeverity(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static AnomalySeverity fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return MEDIUM;
        }
        for (AnomalySeverity s : AnomalySeverity.values()) {
            if (s.name().equalsIgnoreCase(text.trim()) || s.value.equalsIgnoreCase(text.trim())) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown anomaly severity: '" + text + "'. Supported values: LOW, MEDIUM, HIGH, CRITICAL");
    }
}
