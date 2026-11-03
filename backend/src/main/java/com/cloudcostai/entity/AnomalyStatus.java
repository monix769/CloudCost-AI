package com.cloudcostai.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AnomalyStatus {
    OPEN("OPEN"),
    ACKNOWLEDGED("ACKNOWLEDGED"),
    RESOLVED("RESOLVED"),
    IGNORED("IGNORED");

    private final String value;

    AnomalyStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static AnomalyStatus fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return OPEN;
        }
        for (AnomalyStatus s : AnomalyStatus.values()) {
            if (s.name().equalsIgnoreCase(text.trim()) || s.value.equalsIgnoreCase(text.trim())) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown anomaly status: '" + text + "'. Supported values: OPEN, ACKNOWLEDGED, RESOLVED, IGNORED");
    }
}
