package com.cloudcostai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Aggregated cloud cost summary")
public class CostSummaryResponse {

    @Schema(description = "Total expenditure across all accounts within period", example = "43250.75")
    private BigDecimal totalCost;

    @Schema(description = "Primary monetary currency", example = "USD")
    @Builder.Default
    private String currency = "USD";

    @Schema(description = "Start of queried date window")
    private LocalDate startDate;

    @Schema(description = "End of queried date window")
    private LocalDate endDate;

    @Schema(description = "Total count of billing records aggregated", example = "350")
    private Long totalRecords;

    @Schema(description = "Average expenditure per active day in window", example = "1441.69")
    private BigDecimal averageDailyCost;
}
