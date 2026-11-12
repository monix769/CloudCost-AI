package com.cloudcostai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptimizationRecommendationResponse {
    private Long id;
    private Long cloudAccountId;
    private String cloudAccountName;
    private String cloudProvider;
    private String resourceId;
    private String resourceName;
    private String resourceType;
    private String recommendationType;
    private String currentConfig;
    private String suggestedConfig;
    private String priority;
    private String status;
    private String confidenceLevel;
    private String reason;
    private BigDecimal currentMonthlyCost;
    private BigDecimal recommendedMonthlyCost;
    private BigDecimal estimatedMonthlySavings;
    private BigDecimal estimatedAnnualSavings;
    private BigDecimal realizedMonthlySavings;
    private BigDecimal realizedAnnualSavings;
    private BigDecimal savingsPercentage;
    private String currency;
    private String implementationNotes;
    private Instant detectedAt;
    private Instant updatedAt;

    // Optional telemetry enrichment
    private BigDecimal cpuUtilizationAvg;
    private BigDecimal cpuUtilizationMax;
    private BigDecimal memoryUtilizationAvg;
    private BigDecimal memoryUtilizationMax;
    private boolean hasTelemetryData;
}

