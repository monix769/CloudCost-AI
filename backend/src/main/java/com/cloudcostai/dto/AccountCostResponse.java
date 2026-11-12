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
@Schema(description = "Cloud account expenditure breakdown response")
public class AccountCostResponse {

    @Schema(description = "Cloud account internal ID", example = "10")
    private Long accountId;

    @Schema(description = "Provider account identifier", example = "123456789012")
    private String accountNumber;

    @Schema(description = "Account friendly display name", example = "Production AWS Workload")
    private String accountName;

    @Schema(description = "Cloud provider", example = "AWS")
    private CloudProvider provider;

    @Schema(description = "Total expenditure accumulated by account", example = "18400.00")
    private BigDecimal totalCost;

    @Schema(description = "Percentage share of total organization expenditure", example = "42.50")
    private BigDecimal percentage;

    @Schema(description = "Count of billing records attributed to this account", example = "350")
    private Long recordCount;
}

