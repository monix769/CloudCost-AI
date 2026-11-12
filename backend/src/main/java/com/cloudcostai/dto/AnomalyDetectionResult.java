package com.cloudcostai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Diagnostic summary resulting from an anomaly detection execution")
public class AnomalyDetectionResult {

    @Schema(description = "Total potential anomaly candidates evaluated", example = "15")
    private int evaluatedCount;

    @Schema(description = "Total newly created and persisted anomalies", example = "3")
    private int createdCount;

    @Schema(description = "Total duplicate or already active anomalies safely skipped", example = "2")
    private int skippedCount;

    @Schema(description = "Execution scan duration in milliseconds", example = "42")
    private long executionDurationMs;

    @Schema(description = "Human-readable status summary message", example = "Detection complete: 3 anomalies detected and alerted, 2 duplicates skipped.")
    private String summaryMessage;

    @Schema(description = "List of newly generated anomalies from this run")
    private List<CostAnomalyResponse> newlyDetectedAnomalies;
}

