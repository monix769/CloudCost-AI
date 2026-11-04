package com.cloudcostai.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum UserRole {
    ROLE_ADMIN("ROLE_ADMIN"),
    ROLE_USER("ROLE_USER");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Converts a role string (e.g. "ROLE_ADMIN", "ADMIN", "ROLE_USER", "USER", "FINOPS_ANALYST")
     * into a supported UserRole enum safely.
     */
    @JsonCreator
    public static UserRole fromString(String roleStr) {
        if (roleStr == null || roleStr.trim().isEmpty()) {
            return ROLE_USER;
        }
        String clean = roleStr.trim().toUpperCase();
        if (clean.equals("ADMIN") || clean.equals("ROLE_ADMIN")) {
            return ROLE_ADMIN;
        }
        if (clean.equals("USER") || clean.equals("ROLE_USER") || clean.equals("FINOPS_ANALYST")) {
            return ROLE_USER;
        }
        for (UserRole r : UserRole.values()) {
            if (r.name().equalsIgnoreCase(clean)) {
                return r;
            }
        }
        return ROLE_USER;
    }
}
