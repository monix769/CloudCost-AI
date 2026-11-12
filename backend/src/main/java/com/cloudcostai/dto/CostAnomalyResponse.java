package com.cloudcostai.dto;

import com.cloudcostai.entity.AnomalyDetectionMethod;
import com.cloudcostai.entity.AnomalySeverity;
import com.cloudcostai.entity.AnomalyStatus;
import com.cloudcostai.entity.CloudProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Details of an identified spending anomaly")
public class CostAnomalyResponse {

    @Schema(description = "Anomaly record ID", example = "1")
    private Long id;

    @Schema(description = "Organization ID", example = "1")
    private Long organizationId;

    @Schema(description = "Associated Cloud Account ID", example = "1")
    private Long cloudAccountId;

    @Schema(description = "Cloud Provider", example = "AWS")
    private CloudProvider provider;

    @Schema(description = "Account Name", example = "Production AWS Account")
    private String accountName;

    @Schema(description = "Service exhibiting abnormal spend", example = "Amazon RDS")
    private String serviceName;

    @Schema(description = "Geographic cloud region", example = "us-east-1")
    private String region;

    @Schema(description = "Baseline expected cost", example = "120.0000")
    private BigDecimal expectedCost;

    @Schema(description = "Actual recorded expenditure", example = "580.0000")
    private BigDecimal actualCost;

    @Schema(description = "Excess financial cost over expected baseline", example = "460.0000")
    private BigDecimal excessCost;

    @Schema(description = "Percentage deviation from baseline", example = "383.33")
    private BigDecimal deviationPercentage;

    @Schema(description = "Anomaly severity level", example = "HIGH")
    private AnomalySeverity severity;

    @Schema(description = "Statistical detection algorithm used", example = "Z_SCORE")
    private AnomalyDetectionMethod detectionMethod;

    @Schema(description = "Timestamp when the anomaly was flagged")
    private Instant detectedAt;

    @Schema(description = "Period start date for the anomaly window")
    private LocalDate anomalyPeriodStart;

    @Schema(description = "Period end date for the anomaly window")
    private LocalDate anomalyPeriodEnd;

    @Schema(description = "Unique resource / dimension deduplication key")
    private String dimensionKey;

    @Schema(description = "Workflow lifecycle status", example = "OPEN")
    private AnomalyStatus status;

    @Schema(description = "FinOps root cause explanation", example = "Sudden unindexed queries causing Multi-AZ read replica auto-scaling burst.")
    private String rootCauseSummary;

    @Schema(description = "Actionable FinOps corrective recommendation", example = "Review slow query logs and adjust RDS autoscaling max threshold.")
    private String recommendedAction;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;
}
