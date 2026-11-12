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
import java.time.LocalDate;

@Entity
@Table(name = "cost_anomalies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostAnomaly extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cloud_account_id", nullable = false)
    private CloudAccount cloudAccount;

    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;

    @Column(name = "region", length = 50)
    private String region;

    @Column(name = "expected_cost", nullable = false, precision = 14, scale = 4)
    private BigDecimal expectedCost;

    @Column(name = "actual_cost", nullable = false, precision = 14, scale = 4)
    private BigDecimal actualCost;

    @Column(name = "excess_cost", nullable = false, precision = 14, scale = 4)
    @Builder.Default
    private BigDecimal excessCost = BigDecimal.ZERO;

    @Column(name = "deviation_percentage", nullable = false, precision = 6, scale = 2)
    private BigDecimal deviationPercentage;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private AnomalySeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "detection_method", nullable = false, length = 50)
    @Builder.Default
    private AnomalyDetectionMethod detectionMethod = AnomalyDetectionMethod.Z_SCORE;

    @Column(name = "detected_at", nullable = false)
    @Builder.Default
    private Instant detectedAt = Instant.now();

    @Column(name = "anomaly_period_start")
    private LocalDate anomalyPeriodStart;

    @Column(name = "anomaly_period_end")
    private LocalDate anomalyPeriodEnd;

    @Column(name = "dimension_key", length = 255)
    private String dimensionKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AnomalyStatus status = AnomalyStatus.OPEN;

    @Column(name = "root_cause_summary", columnDefinition = "TEXT")
    private String rootCauseSummary;

    @Column(name = "recommended_action", columnDefinition = "TEXT")
    private String recommendedAction;
}
