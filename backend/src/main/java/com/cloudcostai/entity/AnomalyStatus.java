package com.cloudcostai.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AnomalyStatus {
    OPEN("OPEN"),
    ACKNOWLEDGED("ACKNOWLEDGED"),
    RESOLVED("RESOLVED"),
    DISMISSED("DISMISSED"),
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
        String clean = text.trim();
        for (AnomalyStatus s : AnomalyStatus.values()) {
            if (s.name().equalsIgnoreCase(clean) || s.value.equalsIgnoreCase(clean)) {
                return s;
            }
        }
        // Handle common FinOps synonyms
        if ("DISMISS".equalsIgnoreCase(clean)) {
            return DISMISSED;
        }
        if ("IGNORE".equalsIgnoreCase(clean)) {
            return DISMISSED;
        }
        throw new IllegalArgumentException("Unknown anomaly status: '" + text + "'. Supported values: OPEN, ACKNOWLEDGED, RESOLVED, DISMISSED, IGNORED");
    }
}
