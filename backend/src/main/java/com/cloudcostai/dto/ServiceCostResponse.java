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
@Schema(description = "Expenditure aggregation grouped by cloud service")
public class ServiceCostResponse {

    @Schema(description = "Cloud Service Name", example = "Amazon EC2")
    private String serviceName;

    @Schema(description = "Associated Cloud Provider", example = "AWS")
    private CloudProvider provider;

    @Schema(description = "Total expenditure allocated to this service", example = "12400.00")
    private BigDecimal totalCost;

    @Schema(description = "Percentage share of total cloud spend", example = "28.67")
    private BigDecimal percentage;

    @Schema(description = "Total number of billing entries for this service", example = "75")
    private Long recordCount;
}
