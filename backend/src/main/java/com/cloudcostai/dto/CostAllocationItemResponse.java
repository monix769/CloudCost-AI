package com.cloudcostai.dto;

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
@Schema(description = "Individual slice in cost allocation breakdown")
public class CostAllocationItemResponse {

    @Schema(description = "Primary allocation dimension key (e.g., service name, account name, region)", example = "AmazonEC2")
    private String key;

    @Schema(description = "Secondary dimension attribute (e.g. provider or category)", example = "AWS")
    private String subKey;

    @Schema(description = "Allocated cost amount", example = "4250.00")
    private BigDecimal cost;

    @Schema(description = "Percentage share of total allocated expenditure", example = "35.50")
    private BigDecimal percentage;

    @Schema(description = "Number of underlying billing records attributed to this dimension", example = "120")
    private Long recordCount;
}

