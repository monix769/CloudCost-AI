package com.cloudcostai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommitmentAnalysisResponse {
    private int evaluatedDays;
    private BigDecimal totalComputeSpend;
    private BigDecimal baselineMonthlyComputeSpend;
    private BigDecimal recommended1YearSavingsPlanMonthly;
    private BigDecimal estimated1YearSavingsPercent;
    private BigDecimal estimated1YearMonthlySavings;
    private BigDecimal estimated1YearAnnualSavings;
    private BigDecimal recommended3YearSavingsPlanMonthly;
    private BigDecimal estimated3YearSavingsPercent;
    private BigDecimal estimated3YearMonthlySavings;
    private BigDecimal estimated3YearAnnualSavings;
    private List<String> assumptions;
    private String currency;
}

