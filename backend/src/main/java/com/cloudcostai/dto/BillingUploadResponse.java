package com.cloudcostai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Summary of CSV batch ingestion results")
public class BillingUploadResponse {

    @Schema(description = "Total number of CSV rows evaluated", example = "50")
    private int totalProcessed;

    @Schema(description = "Count of successfully saved billing records", example = "48")
    private int importedCount;

    @Schema(description = "Count of detected duplicate records skipped", example = "2")
    private int duplicateCount;

    @Schema(description = "Count of rows failed due to validation or formatting errors", example = "0")
    private int failedCount;

    @Schema(description = "List of row-level error descriptions")
    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @Schema(description = "Human-readable summary", example = "Successfully imported 48 records. 2 duplicates skipped.")
    private String summary;
}
