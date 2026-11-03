package com.cloudcostai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
@Schema(description = "Payload to record a single cloud billing entry")
public class BillingRecordRequest {

    @NotNull(message = "Cloud account ID is required")
    @Schema(description = "ID of the associated CloudAccount", example = "1")
    private Long cloudAccountId;

    @NotBlank(message = "Service name is required")
    @Size(max = 100, message = "Service name cannot exceed 100 characters")
    @Schema(description = "Cloud service name", example = "Amazon EC2")
    private String serviceName;

    @NotBlank(message = "Region is required")
    @Size(max = 60, message = "Region cannot exceed 60 characters")
    @Schema(description = "Provider geographic region", example = "us-east-1")
    private String region;

    @NotNull(message = "Cost amount is required")
    @DecimalMin(value = "0.0000", inclusive = true, message = "Cost amount must be greater than or equal to 0.0000")
    @Schema(description = "Calculated expenditure for this line item", example = "142.5000")
    private BigDecimal costAmount;

    @Size(max = 10, message = "Currency code cannot exceed 10 characters")
    @Schema(description = "Currency ISO code", example = "USD")
    @Builder.Default
    private String currency = "USD";

    @Size(max = 120, message = "Usage type cannot exceed 120 characters")
    @Schema(description = "Provider usage metric", example = "BoxUsage:t3.medium")
    private String usageType;

    @DecimalMin(value = "0.0000", inclusive = true, message = "Usage quantity cannot be negative")
    @Schema(description = "Total units consumed", example = "720.0000")
    private BigDecimal usageQuantity;

    @Size(max = 50, message = "Usage unit cannot exceed 50 characters")
    @Schema(description = "Measurement unit", example = "Hrs")
    private String usageUnit;

    @NotNull(message = "Billing period start date is required")
    @Schema(description = "Inclusive start date (YYYY-MM-DD)", example = "2026-05-01")
    private LocalDate billingPeriodStart;

    @NotNull(message = "Billing period end date is required")
    @Schema(description = "Inclusive end date (YYYY-MM-DD)", example = "2026-05-31")
    private LocalDate billingPeriodEnd;

    @Size(max = 255, message = "Resource ID cannot exceed 255 characters")
    @Schema(description = "Optional ARN or resource identifier", example = "i-0987654321fedcba")
    private String resourceId;

    @Schema(description = "Optional tags or key-value JSON string", example = "{\"env\":\"prod\",\"team\":\"finops\"}")
    private String tags;
}
