package com.cloudcostai.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CloudProvider {
    AWS("AWS"),
    AZURE("AZURE"),
    GCP("GCP");

    private final String value;

    CloudProvider(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static CloudProvider fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Cloud provider cannot be null or empty. Supported: AWS, AZURE, GCP");
        }
        for (CloudProvider b : CloudProvider.values()) {
            if (b.name().equalsIgnoreCase(text.trim()) || b.value.equalsIgnoreCase(text.trim())) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unknown cloud provider: '" + text + "'. Supported providers are: AWS, AZURE, GCP");
    }
}
