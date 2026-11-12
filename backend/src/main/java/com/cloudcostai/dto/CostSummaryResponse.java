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
@Schema(description = "Aggregated cloud cost summary with FinOps velocity indicators")
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

    @Schema(description = "Expenditure in current calendar month to date", example = "12800.50")
    private BigDecimal currentMonthCost;

    @Schema(description = "Expenditure in previous calendar month", example = "11400.00")
    private BigDecimal previousMonthCost;

    @Schema(description = "Date of peak spending within queried range", example = "2026-10-18")
    private LocalDate highestSpendingDate;

    @Schema(description = "Cost amount incurred on peak spending day", example = "2150.00")
    private BigDecimal highestSpendingDayCost;

    @Schema(description = "Date of lowest spending within queried range", example = "2026-10-04")
    private LocalDate lowestSpendingDate;

    @Schema(description = "Cost amount incurred on lowest spending day", example = "850.25")
    private BigDecimal lowestSpendingDayCost;
}
