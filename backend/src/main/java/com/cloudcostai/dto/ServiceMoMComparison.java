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
@Schema(description = "Service-level month-over-month expenditure comparison")
public class ServiceMoMComparison {

    @Schema(description = "Cloud service name", example = "AmazonEC2")
    private String serviceName;

    @Schema(description = "Cloud provider", example = "AWS")
    private CloudProvider provider;

    @Schema(description = "Current month expenditure", example = "5400.00")
    private BigDecimal currentMonthCost;

    @Schema(description = "Previous month expenditure", example = "4800.00")
    private BigDecimal previousMonthCost;

    @Schema(description = "Absolute change (current - previous)", example = "600.00")
    private BigDecimal absoluteChange;

    @Schema(description = "Percentage change relative to previous month", example = "12.50")
    private BigDecimal percentageChange;
}

