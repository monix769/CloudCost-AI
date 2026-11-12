package com.cloudcostai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "In-app alert notification details")
public class NotificationResponse {

    @Schema(description = "Notification ID", example = "1")
    private Long id;

    @Schema(description = "Notification headline", example = "CRITICAL: Amazon RDS Cost Anomaly Detected")
    private String title;

    @Schema(description = "Detailed alert message body", example = "Spend surged by +350.00% over baseline ($450.00 vs $100.00).")
    private String message;

    @Schema(description = "Alert severity level", example = "CRITICAL")
    private String severity;

    @Schema(description = "Category of notification", example = "ANOMALY_DETECTED")
    private String notificationType;

    @Schema(description = "Referenced domain entity ID (e.g. anomaly ID)", example = "1")
    private Long referenceId;

    @Schema(description = "Referenced domain entity type", example = "COST_ANOMALY")
    private String referenceType;

    @Schema(description = "Whether the alert has been acknowledged/read", example = "false")
    private Boolean isRead;

    @Schema(description = "Timestamp when notification was read")
    private Instant readAt;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;
}

