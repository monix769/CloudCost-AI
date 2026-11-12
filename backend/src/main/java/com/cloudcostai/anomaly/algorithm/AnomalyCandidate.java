package com.cloudcostai.anomaly.algorithm;

import com.cloudcostai.entity.AnomalyDetectionMethod;
import com.cloudcostai.entity.AnomalySeverity;
import com.cloudcostai.entity.CloudAccount;
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
public class AnomalyCandidate {
    private CloudAccount cloudAccount;
    private String serviceName;
    private String region;
    private BigDecimal expectedCost;
    private BigDecimal actualCost;
    private BigDecimal excessCost;
    private BigDecimal deviationPercentage;
    private AnomalySeverity severity;
    private AnomalyDetectionMethod detectionMethod;
    private LocalDate anomalyPeriodStart;
    private LocalDate anomalyPeriodEnd;
    private String dimensionKey;
    private String rootCauseSummary;
    private String recommendedAction;
}

