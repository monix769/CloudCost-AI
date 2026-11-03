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

@Entity
@Table(name = "budgets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Budget extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "amount_limit", nullable = false, precision = 14, scale = 2)
    private BigDecimal amountLimit;

    @Column(name = "current_spend", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal currentSpend = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "period", nullable = false, length = 20)
    @Builder.Default
    private BudgetPeriod period = BudgetPeriod.MONTHLY;

    @Column(name = "alert_threshold_percentage", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal alertThresholdPercentage = new BigDecimal("80.00");

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "ACTIVE";
}
