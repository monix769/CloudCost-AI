package com.cloudcostai.dto;

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
public class OptimizationOverviewResponse {
    private BigDecimal totalPotentialMonthlySavings;
    private BigDecimal totalPotentialAnnualSavings;
    private BigDecimal totalRealizedMonthlySavings;
    private BigDecimal totalRealizedAnnualSavings;
    private long totalRecommendationsCount;
    private long newRecommendationsCount;
    private long approvedRecommendationsCount;
    private long implementedRecommendationsCount;
    private long criticalPriorityCount;
    private long highPriorityCount;
    private long mediumPriorityCount;
    private long lowPriorityCount;
    private String currency;
}

