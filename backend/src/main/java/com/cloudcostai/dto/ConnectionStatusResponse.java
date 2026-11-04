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
@Schema(description = "Current connection and sync status for a cloud account")
public class ConnectionStatusResponse {

    @Schema(description = "Cloud Account ID", example = "1")
    private Long cloudAccountId;

    @Schema(description = "Provider", example = "AWS")
    private String provider;

    @Schema(description = "Connection status (DISCONNECTED, PENDING, CONNECTED, FAILED)", example = "CONNECTED")
    private String connectionStatus;

    @Schema(description = "Sync status (PENDING, SYNCING, SYNCED, FAILED)", example = "SYNCED")
    private String syncStatus;

    @Schema(description = "Whether encrypted credentials are configured")
    private boolean credentialsConfigured;

    @Schema(description = "Encryption key version used for credentials", example = "v1")
    private String keyVersion;

    @Schema(description = "Timestamp of last synchronization")
    private Instant lastSyncedAt;

    @Schema(description = "Whether a synchronization task is currently running")
    private boolean isSyncInProgress;
}
