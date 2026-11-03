package com.cloudcostai.dto;

import com.cloudcostai.entity.CloudProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Details of an individual persisted billing line item")
public class BillingRecordResponse {

    @Schema(description = "Record ID", example = "101")
    private Long id;

    @Schema(description = "Organization ID", example = "1")
    private Long organizationId;

    @Schema(description = "Associated Cloud Account ID", example = "1")
    private Long cloudAccountId;

    @Schema(description = "Cloud Provider", example = "AWS")
    private CloudProvider provider;

    @Schema(description = "Cloud Account Name", example = "Production AWS Account")
    private String accountName;

    @Schema(description = "Service Name", example = "Amazon EC2")
    private String serviceName;

    @Schema(description = "Provider Region", example = "us-east-1")
    private String region;

    @Schema(description = "Total line-item cost", example = "142.5000")
    private BigDecimal costAmount;

    @Schema(description = "Monetary currency", example = "USD")
    private String currency;

    @Schema(description = "Usage Type", example = "BoxUsage:t3.medium")
    private String usageType;

    @Schema(description = "Usage Quantity", example = "720.0000")
    private BigDecimal usageQuantity;

    @Schema(description = "Usage Unit", example = "Hrs")
    private String usageUnit;

    @Schema(description = "Billing Period Start", example = "2026-05-01")
    private LocalDate billingPeriodStart;

    @Schema(description = "Billing Period End", example = "2026-05-31")
    private LocalDate billingPeriodEnd;

    @Schema(description = "Cloud Resource ID", example = "i-0987654321fedcba")
    private String resourceId;

    @Schema(description = "Resource tags / metadata JSON")
    private String tags;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;
}
