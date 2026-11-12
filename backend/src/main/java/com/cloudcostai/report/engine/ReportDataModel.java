package com.cloudcostai.report.engine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportDataModel {
    private String reportTitle;
    private String organizationName;
    private LocalDate startDate;
    private LocalDate endDate;
    @Builder.Default
    private String currency = "USD";
    @Builder.Default
    private LocalDate generatedAt = LocalDate.now();
    private String executiveSummary;
    private BigDecimal totalSpend;
    private long anomalyCount;
    private BigDecimal potentialMonthlySavings;
    private BigDecimal realizedMonthlySavings;
    private List<String> tableHeaders;
    private List<List<String>> tableRows;
    private List<String> insights;
}

