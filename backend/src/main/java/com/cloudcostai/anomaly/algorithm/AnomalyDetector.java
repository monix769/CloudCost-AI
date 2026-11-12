package com.cloudcostai.anomaly.algorithm;

import com.cloudcostai.entity.AnomalyDetectionConfig;
import com.cloudcostai.entity.AnomalyDetectionMethod;
import com.cloudcostai.entity.CloudAccount;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Strategy contract for FinOps anomaly detection algorithms.
 */
public interface AnomalyDetector {

    /**
     * Identifies the detection method supported by this detector.
     */
    AnomalyDetectionMethod getMethod();

    /**
     * Evaluates historical time-series spend data for an account and service,
     * identifying cost outliers and anomalous spikes.
     *
     * @param account       The cloud account owning the workload
     * @param serviceName   The affected service name
     * @param region        The cloud region (optional)
     * @param dailyCosts    Chronologically ordered map of date to daily cost
     * @param targetDate    The date being evaluated for anomalies
     * @param config        Tenant-configured thresholds
     * @return List of detected anomaly candidates (usually 0 or 1 per target date)
     */
    List<AnomalyCandidate> detect(
            CloudAccount account,
            String serviceName,
            String region,
            Map<LocalDate, BigDecimal> dailyCosts,
            LocalDate targetDate,
            AnomalyDetectionConfig config
    );
}

