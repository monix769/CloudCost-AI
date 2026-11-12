package com.cloudcostai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body to update organization anomaly detection parameters")
public class AnomalyConfigRequest {

    @JsonProperty("zScoreThreshold")
    @NotNull(message = "Z-Score threshold cannot be null")
    @DecimalMin(value = "1.00", message = "Z-Score threshold must be at least 1.00")
    @DecimalMax(value = "5.00", message = "Z-Score threshold cannot exceed 5.00")
    @Schema(description = "Z-Score standard deviation threshold", example = "2.50")
    private BigDecimal zScoreThreshold;

    @NotNull(message = "Moving average window days cannot be null")
    @Min(value = 3, message = "Window must be at least 3 days")
    @Max(value = 90, message = "Window cannot exceed 90 days")
    @Schema(description = "Rolling window size in days", example = "14")
    private Integer movingAvgWindowDays;

    @NotNull(message = "Moving average deviation percent cannot be null")
    @DecimalMin(value = "5.00")
    @DecimalMax(value = "500.00")
    @Schema(description = "Moving average deviation percentage threshold", example = "30.00")
    private BigDecimal movingAvgDeviationPercent;

    @NotNull(message = "Spending spike percent cannot be null")
    @DecimalMin(value = "10.00")
    @DecimalMax(value = "1000.00")
    @Schema(description = "Daily spending spike percentage threshold", example = "50.00")
    private BigDecimal spendingSpikePercent;

    @NotNull(message = "Spending spike minimum dollar amount cannot be null")
    @DecimalMin(value = "0.00")
    @Schema(description = "Minimum excess dollar spend to trigger spike alert", example = "50.0000")
    private BigDecimal spendingSpikeMinAmount;

    @NotNull(message = "Budget warning threshold cannot be null")
    @DecimalMin(value = "50.00")
    @DecimalMax(value = "99.00")
    @Schema(description = "Budget warning utilization threshold percentage", example = "90.00")
    private BigDecimal budgetWarningThresholdPercent;

    @NotNull(message = "Budget critical threshold cannot be null")
    @DecimalMin(value = "80.00")
    @DecimalMax(value = "200.00")
    @Schema(description = "Budget critical utilization threshold percentage", example = "100.00")
    private BigDecimal budgetCriticalThresholdPercent;

    @Schema(description = "Toggle to enable or disable background scheduled detection", example = "true")
    private Boolean isAutomatedDetectionEnabled;
}

