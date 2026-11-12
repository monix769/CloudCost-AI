package com.cloudcostai.optimization.model;

import com.cloudcostai.entity.CloudAccount;
import com.cloudcostai.entity.CloudProvider;
import com.cloudcostai.entity.OptimizationConfidence;
import com.cloudcostai.entity.OptimizationPriority;
import com.cloudcostai.entity.OptimizationRecommendationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateRecommendation {
    private CloudAccount cloudAccount;
    private String resourceId;
    private String resourceName;
    private String resourceType;
    private CloudProvider cloudProvider;
    private OptimizationRecommendationType recommendationType;
    private String currentConfig;
    private String suggestedConfig;
    private OptimizationPriority priority;
    private OptimizationConfidence confidenceLevel;
    private String reason;
    private BigDecimal currentMonthlyCost;
    private BigDecimal recommendedMonthlyCost;
    private BigDecimal estimatedMonthlySavings;
    private BigDecimal estimatedAnnualSavings;
    @Builder.Default
    private String currency = "USD";
}

