package com.cloudcostai.dto;

import com.cloudcostai.entity.CloudProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Expenditure aggregation grouped by cloud provider")
public class ProviderCostResponse {

    @Schema(description = "Cloud Provider", example = "AWS")
    private CloudProvider provider;

    @Schema(description = "Total expenditure allocated to this provider", example = "24500.50")
    private BigDecimal totalCost;

    @Schema(description = "Percentage share of total cloud spend", example = "56.65")
    private BigDecimal percentage;

    @Schema(description = "Total number of billing line items for this provider", example = "180")
    private Long recordCount;
}
