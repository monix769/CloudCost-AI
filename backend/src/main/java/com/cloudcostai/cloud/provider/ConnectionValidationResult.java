package com.cloudcostai.cloud.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionValidationResult {
    private boolean valid;
    private String connectionStatus;
    private String message;
    @Builder.Default
    private List<String> validatedPermissions = new ArrayList<>();
    @Builder.Default
    private List<String> missingPermissions = new ArrayList<>();
    private String providerAccountId;
    @Builder.Default
    private Instant validatedAt = Instant.now();
    private boolean mockValidation;
}
