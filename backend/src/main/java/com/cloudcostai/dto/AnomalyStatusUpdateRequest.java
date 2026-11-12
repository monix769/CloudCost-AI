package com.cloudcostai.dto;

import com.cloudcostai.entity.AnomalyStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for updating anomaly workflow status")
public class AnomalyStatusUpdateRequest {

    @NotNull(message = "Status cannot be null")
    @Schema(description = "Target status (OPEN, ACKNOWLEDGED, RESOLVED, DISMISSED, IGNORED)", example = "ACKNOWLEDGED")
    private AnomalyStatus status;

    @Schema(description = "Optional notes or resolution comments from FinOps operator", example = "Investigated RDS instance. Autoscaling burst resolved by indexing key tables.")
    private String notes;
}
