package com.cloudcostai.dto;

import com.cloudcostai.entity.CloudProvider;
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
@Schema(description = "Details of a registered cloud account")
public class CloudAccountResponse {

    @Schema(description = "Primary key ID", example = "1")
    private Long id;

    @Schema(description = "Organization ID", example = "1")
    private Long organizationId;

    @Schema(description = "Cloud provider", example = "AWS")
    private CloudProvider provider;

    @Schema(description = "Provider account identifier", example = "123456789012")
    private String accountId;

    @Schema(description = "Account descriptive name", example = "Production AWS Account")
    private String accountName;

    @Schema(description = "Associated IAM ARN or client ID")
    private String arnOrClientId;

    @Schema(description = "Provider synchronization status", example = "ACTIVE")
    private String syncStatus;

    @Schema(description = "Timestamp of last synchronization")
    private Instant lastSyncedAt;

    @Schema(description = "Whether account is currently active", example = "true")
    private Boolean isActive;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Last update timestamp")
    private Instant updatedAt;
}
