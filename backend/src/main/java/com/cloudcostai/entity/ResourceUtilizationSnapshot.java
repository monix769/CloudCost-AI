package com.cloudcostai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "resource_utilization_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceUtilizationSnapshot extends BaseEntity {

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

    @Column(name = "region", length = 50)
    private String region;

    @Column(name = "cpu_utilization_avg", precision = 5, scale = 2)
    private BigDecimal cpuUtilizationAvg;

    @Column(name = "cpu_utilization_max", precision = 5, scale = 2)
    private BigDecimal cpuUtilizationMax;

    @Column(name = "memory_utilization_avg", precision = 5, scale = 2)
    private BigDecimal memoryUtilizationAvg;

    @Column(name = "memory_utilization_max", precision = 5, scale = 2)
    private BigDecimal memoryUtilizationMax;

    @Column(name = "running_hours", nullable = false, precision = 7, scale = 2)
    @Builder.Default
    private BigDecimal runningHours = new BigDecimal("730.00");

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;
}

