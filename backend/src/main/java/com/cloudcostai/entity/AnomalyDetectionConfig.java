package com.cloudcostai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "anomaly_detection_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnomalyDetectionConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, unique = true)
    private Organization organization;

    @Column(name = "z_score_threshold", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal zScoreThreshold = new BigDecimal("2.50");

    @Column(name = "moving_avg_window_days", nullable = false)
    @Builder.Default
    private Integer movingAvgWindowDays = 14;

    @Column(name = "moving_avg_deviation_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal movingAvgDeviationPercent = new BigDecimal("30.00");

    @Column(name = "spending_spike_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal spendingSpikePercent = new BigDecimal("50.00");

    @Column(name = "spending_spike_min_amount", nullable = false, precision = 14, scale = 4)
    @Builder.Default
    private BigDecimal spendingSpikeMinAmount = new BigDecimal("50.0000");

    @Column(name = "budget_warning_threshold_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal budgetWarningThresholdPercent = new BigDecimal("90.00");

    @Column(name = "budget_critical_threshold_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal budgetCriticalThresholdPercent = new BigDecimal("100.00");

    @Column(name = "is_automated_detection_enabled", nullable = false)
    @Builder.Default
    private Boolean isAutomatedDetectionEnabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}

