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
@Schema(description = "Payload to transition an anomaly's operational status")
public class AnomalyStatusUpdateRequest {

    @NotNull(message = "Status is required (OPEN, ACKNOWLEDGED, RESOLVED, IGNORED)")
    @Schema(description = "Target anomaly status", example = "ACKNOWLEDGED")
    private AnomalyStatus status;
}
