package com.cloudcostai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Single temporal data point for cost anomaly time-series trends")
public class AnomalyTrendPointResponse {

    @Schema(description = "Date of occurrence", example = "2026-11-10")
    private LocalDate date;

    @Schema(description = "Total anomalies detected on this date", example = "3")
    private int totalAnomalies;

    @Schema(description = "Count of CRITICAL severity anomalies on this date", example = "1")
    private int criticalCount;

    @Schema(description = "Count of HIGH severity anomalies on this date", example = "2")
    private int highCount;

    @Schema(description = "Aggregate excess cost identified on this date", example = "1250.00")
    private BigDecimal excessCost;
}

