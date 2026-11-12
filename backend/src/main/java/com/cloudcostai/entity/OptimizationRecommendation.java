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
import java.time.Instant;

@Entity
@Table(name = "optimization_recommendations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptimizationRecommendation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cloud_account_id", nullable = false)
    private CloudAccount cloudAccount;

    @Column(name = "resource_id", nullable = false, length = 255)
    private String resourceId;

    @Column(name = "resource_name", length = 255)
    private String resourceName;

    @Column(name = "resource_type", nullable = false, length = 100)
    private String resourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "cloud_provider", nullable = false, length = 20)
    private CloudProvider cloudProvider;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation_type", nullable = false, length = 50)
    private OptimizationRecommendationType recommendationType;

    @Column(name = "current_config", nullable = false, length = 255)
    private String currentConfig;

    @Column(name = "suggested_config", nullable = false, length = 255)
    private String suggestedConfig;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private OptimizationPriority priority = OptimizationPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private OptimizationStatus status = OptimizationStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_level", nullable = false, length = 20)
    @Builder.Default
    private OptimizationConfidence confidenceLevel = OptimizationConfidence.MEDIUM;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "current_monthly_cost", nullable = false, precision = 14, scale = 4)
    private BigDecimal currentMonthlyCost;

    @Column(name = "recommended_monthly_cost", nullable = false, precision = 14, scale = 4)
    private BigDecimal recommendedMonthlyCost;

    @Column(name = "estimated_monthly_savings", nullable = false, precision = 14, scale = 4)
    private BigDecimal estimatedMonthlySavings;

    @Column(name = "estimated_annual_savings", nullable = false, precision = 14, scale = 4)
    private BigDecimal estimatedAnnualSavings;

    @Column(name = "realized_monthly_savings", nullable = false, precision = 14, scale = 4)
    @Builder.Default
    private BigDecimal realizedMonthlySavings = BigDecimal.ZERO;

    @Column(name = "realized_annual_savings", nullable = false, precision = 14, scale = 4)
    @Builder.Default
    private BigDecimal realizedAnnualSavings = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "implementation_notes", columnDefinition = "TEXT")
    private String implementationNotes;

    @Column(name = "detected_at", nullable = false)
    @Builder.Default
    private Instant detectedAt = Instant.now();
}

