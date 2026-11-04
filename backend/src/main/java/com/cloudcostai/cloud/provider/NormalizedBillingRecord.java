package com.cloudcostai.cloud.provider;

import com.cloudcostai.entity.BillingSourceType;
import com.cloudcostai.entity.CloudProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Standardized billing record representation emitted by all cloud provider adapters.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NormalizedBillingRecord {
    private CloudProvider provider;
    private String providerRecordId; // Native record identifier (if supplied by provider)
    private String serviceName;
    private String region;
    private BigDecimal costAmount;
    @Builder.Default
    private String currency = "USD";
    private String usageType;
    private BigDecimal usageQuantity;
    private String usageUnit;
    private LocalDate billingPeriodStart;
    private LocalDate billingPeriodEnd;
    private String resourceId;
    private String tags;
    @Builder.Default
    private BillingSourceType sourceType = BillingSourceType.REAL_PROVIDER;
    private String externalRecordId; // Deterministic fingerprint
}
