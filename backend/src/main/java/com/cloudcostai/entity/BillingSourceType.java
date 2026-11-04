package com.cloudcostai.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Distinguishes the provenance of billing records throughout the platform.
 * Ensures full transparency between real provider ingestions, mock provider ingestions,
 * CSV bulk uploads, and manual line-item entries.
 */
public enum BillingSourceType {
    REAL_PROVIDER("REAL_PROVIDER"),
    MOCK_PROVIDER("MOCK_PROVIDER"),
    CSV_UPLOAD("CSV_UPLOAD"),
    MANUAL("MANUAL");

    private final String value;

    BillingSourceType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static BillingSourceType fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return MANUAL;
        }
        for (BillingSourceType b : BillingSourceType.values()) {
            if (b.name().equalsIgnoreCase(text.trim()) || b.value.equalsIgnoreCase(text.trim())) {
                return b;
            }
        }
        return MANUAL;
    }
}
