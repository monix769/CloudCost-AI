package com.cloudcostai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceUtilizationResponse {
    private Long id;
    private Long cloudAccountId;
    private String cloudAccountName;
    private String cloudProvider;
    private String resourceId;
    private String resourceName;
    private String resourceType;
    private String region;
    private BigDecimal cpuUtilizationAvg;
    private BigDecimal cpuUtilizationMax;
    private BigDecimal memoryUtilizationAvg;
    private BigDecimal memoryUtilizationMax;
    private BigDecimal runningHours;
    private LocalDate snapshotDate;
    private String telemetryStatus; // HEALTHY, UNDERUTILIZED, IDLE, MONITORING_REQUIRED
}

