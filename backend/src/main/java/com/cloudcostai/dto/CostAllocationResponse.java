package com.cloudcostai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Multi-dimensional cloud cost allocation report")
public class CostAllocationResponse {

    @Schema(description = "Allocation dimension analyzed", example = "SERVICE")
    private CostAllocationDimension dimension;

    @Schema(description = "Monetary currency unit", example = "USD")
    @Builder.Default
    private String currency = "USD";

    @Schema(description = "Query window start date")
    private LocalDate startDate;

    @Schema(description = "Query window end date")
    private LocalDate endDate;

    @Schema(description = "Total expenditure allocated across this dimension", example = "45000.00")
    private BigDecimal totalCost;

    @Schema(description = "Breakdown items ordered by descending cost contribution")
    @Builder.Default
    private List<CostAllocationItemResponse> items = new ArrayList<>();
}

