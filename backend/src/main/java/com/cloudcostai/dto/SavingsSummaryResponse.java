package com.cloudcostai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsSummaryResponse {
    private BigDecimal totalPotentialMonthlySavings;
    private BigDecimal totalPotentialAnnualSavings;
    private BigDecimal totalRealizedMonthlySavings;
    private BigDecimal totalRealizedAnnualSavings;
    private Map<String, BigDecimal> byProvider;
    private Map<String, BigDecimal> byRecommendationType;
    private Map<String, BigDecimal> byResourceType;
    private String currency;
}

