package com.cloudcostai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "billing_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cloud_account_id", nullable = false)
    private CloudAccount cloudAccount;

    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;

    @Column(name = "region", nullable = false, length = 60)
    private String region;

    @Column(name = "cost_amount", nullable = false, precision = 14, scale = 4)
    private BigDecimal costAmount;

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "usage_type", length = 120)
    private String usageType;

    @Column(name = "usage_quantity", precision = 16, scale = 4)
    private BigDecimal usageQuantity;

    @Column(name = "usage_unit", length = 50)
    private String usageUnit;

    @Column(name = "billing_period_start", nullable = false)
    private LocalDate billingPeriodStart;

    @Column(name = "billing_period_end", nullable = false)
    private LocalDate billingPeriodEnd;

    @Column(name = "resource_id", length = 255)
    private String resourceId;

    @Column(name = "tags", columnDefinition = "JSON")
    private String tags;

    @Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    @Builder.Default
    private BillingSourceType sourceType = BillingSourceType.MANUAL;

    @Column(name = "external_record_id", length = 255)
    private String externalRecordId;
}
