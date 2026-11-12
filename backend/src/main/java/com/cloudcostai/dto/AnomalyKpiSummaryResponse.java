package com.cloudcostai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Aggregated KPI metrics for cost anomaly operations")
public class AnomalyKpiSummaryResponse {

    @Schema(description = "Total number of anomalies detected", example = "42")
    private long totalAnomalies;

    @Schema(description = "Count of CRITICAL severity anomalies", example = "5")
    private long criticalCount;

    @Schema(description = "Count of HIGH severity anomalies", example = "12")
    private long highCount;

    @Schema(description = "Count of MEDIUM severity anomalies", example = "18")
    private long mediumCount;

    @Schema(description = "Count of LOW severity anomalies", example = "7")
    private long lowCount;

    @Schema(description = "Count of anomalies currently in OPEN state", example = "14")
    private long openCount;

    @Schema(description = "Count of anomalies currently in ACKNOWLEDGED state", example = "8")
    private long acknowledgedCount;

    @Schema(description = "Count of anomalies currently in RESOLVED state", example = "16")
    private long resolvedCount;

    @Schema(description = "Count of anomalies currently in DISMISSED state", example = "4")
    private long dismissedCount;

    @Schema(description = "Total potential excess spending incurred across open/active anomalies", example = "14250.75")
    private BigDecimal totalExcessCost;

    @Schema(description = "Reporting ISO currency code", example = "USD")
    private String currency;
}

