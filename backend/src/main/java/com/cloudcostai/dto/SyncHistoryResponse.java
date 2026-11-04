package com.cloudcostai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Details of a cloud billing synchronization execution")
public class SyncHistoryResponse {

    @Schema(description = "Primary key ID", example = "1")
    private Long id;

    @Schema(description = "Organization ID", example = "1")
    private Long organizationId;

    @Schema(description = "Cloud Account ID", example = "1")
    private Long cloudAccountId;

    @Schema(description = "Cloud Account Name", example = "Production AWS Account")
    private String accountName;

    @Schema(description = "Cloud Provider", example = "AWS")
    private String provider;

    @Schema(description = "Sync execution start timestamp")
    private Instant syncStartTime;

    @Schema(description = "Sync execution completion timestamp")
    private Instant syncEndTime;

    @Schema(description = "Execution status (SUCCESS, FAILED, PARTIAL, IN_PROGRESS)", example = "SUCCESS")
    private String syncStatus;

    @Schema(description = "Number of billing records fetched from provider API", example = "120")
    private Integer recordsFetched;

    @Schema(description = "Number of new billing records inserted into MySQL", example = "45")
    private Integer recordsInserted;

    @Schema(description = "Number of duplicate records skipped via idempotency check", example = "75")
    private Integer recordsSkipped;

    @Schema(description = "Error details if status is FAILED or PARTIAL")
    private String errorSummary;

    @Schema(description = "Queried date range start", example = "2026-05-01")
    private LocalDate dateRangeStart;

    @Schema(description = "Queried date range end", example = "2026-05-31")
    private LocalDate dateRangeEnd;
}
