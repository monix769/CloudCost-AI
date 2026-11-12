package com.cloudcostai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detection parameters configured for the tenant organization")
public class AnomalyConfigResponse {

    @Schema(description = "Organization ID", example = "1")
    private Long organizationId;

    @JsonProperty("zScoreThreshold")
    @Schema(description = "Z-Score standard deviation threshold", example = "2.50")
    private BigDecimal zScoreThreshold;

    @Schema(description = "Rolling window size in days", example = "14")
    private Integer movingAvgWindowDays;

    @Schema(description = "Moving average deviation percentage threshold", example = "30.00")
    private BigDecimal movingAvgDeviationPercent;

    @Schema(description = "Daily spending spike percentage threshold", example = "50.00")
    private BigDecimal spendingSpikePercent;

    @Schema(description = "Minimum excess dollar spend to trigger spike alert", example = "50.0000")
    private BigDecimal spendingSpikeMinAmount;

    @Schema(description = "Budget warning utilization threshold percentage", example = "90.00")
    private BigDecimal budgetWarningThresholdPercent;

    @Schema(description = "Budget critical utilization threshold percentage", example = "100.00")
    private BigDecimal budgetCriticalThresholdPercent;

    @Schema(description = "Whether background scheduled detection is enabled", example = "true")
    private Boolean isAutomatedDetectionEnabled;

    @Schema(description = "Last update timestamp")
    private Instant updatedAt;
}

