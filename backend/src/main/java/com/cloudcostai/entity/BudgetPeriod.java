package com.cloudcostai.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BudgetPeriod {
    MONTHLY("MONTHLY"),
    QUARTERLY("QUARTERLY"),
    ANNUAL("ANNUAL");

    private final String value;

    BudgetPeriod(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static BudgetPeriod fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return MONTHLY;
        }
        for (BudgetPeriod p : BudgetPeriod.values()) {
            if (p.name().equalsIgnoreCase(text.trim()) || p.value.equalsIgnoreCase(text.trim())) {
                return p;
            }
        }
        throw new IllegalArgumentException("Unknown budget period: '" + text + "'. Supported periods are: MONTHLY, QUARTERLY, ANNUAL");
    }
}
