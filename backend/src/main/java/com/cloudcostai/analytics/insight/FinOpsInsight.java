package com.cloudcostai.analytics.insight;

import com.cloudcostai.entity.CloudProvider;
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
@Schema(description = "Actionable rule-based FinOps financial insight")
public class FinOpsInsight {

    @Schema(description = "Unique deterministic insight identifier", example = "INS-MOM-SPIKE-202611")
    private String id;

    @Schema(description = "Concise summary title", example = "Significant Month-over-Month Spending Increase")
    private String title;

    @Schema(description = "Detailed explanatory context of the detected spend anomaly or pattern")
    private String description;

    @Schema(description = "Calculated urgency level", example = "HIGH")
    private InsightSeverity severity;

    @Schema(description = "Related cloud provider", example = "AWS")
    private CloudProvider relatedProvider;

    @Schema(description = "Related cloud service name", example = "AmazonEC2")
    private String relatedService;

    @Schema(description = "Related cloud account ID or name", example = "prod-aws-workload")
    private String relatedAccount;

    @Schema(description = "Estimated financial impact in currency units", example = "2450.00")
    private BigDecimal financialImpact;

    @Schema(description = "Monetary currency", example = "USD")
    @Builder.Default
    private String currency = "USD";

    @Schema(description = "Prescriptive engineering or FinOps recommendation to mitigate or optimize spend")
    private String recommendation;

    @Schema(description = "Category classification", example = "COST_SPIKE")
    private String category;

    @Schema(description = "Timestamp when rule detected pattern")
    @Builder.Default
    private Instant detectionTimestamp = Instant.now();
}

