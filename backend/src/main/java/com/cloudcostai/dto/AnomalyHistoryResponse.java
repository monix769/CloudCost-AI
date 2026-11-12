package com.cloudcostai.dto;

import com.cloudcostai.entity.AnomalyStatus;
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
@Schema(description = "Lifecycle audit event representing an anomaly state transition")
public class AnomalyHistoryResponse {

    @Schema(description = "Audit event ID", example = "1")
    private Long id;

    @Schema(description = "Previous status", example = "OPEN")
    private AnomalyStatus previousStatus;

    @Schema(description = "New transitioned status", example = "ACKNOWLEDGED")
    private AnomalyStatus newStatus;

    @Schema(description = "User ID who performed transition", example = "1")
    private Long actionByUserId;

    @Schema(description = "Email of operator who performed transition", example = "admin@cloudcostai.local")
    private String actionByUserEmail;

    @Schema(description = "Transition notes or reasoning", example = "Acknowledged anomaly. Investigating replica surge.")
    private String notes;

    @Schema(description = "Timestamp when transition took place")
    private Instant createdAt;
}

