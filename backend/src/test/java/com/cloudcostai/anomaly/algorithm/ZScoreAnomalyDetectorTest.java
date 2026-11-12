package com.cloudcostai.anomaly.algorithm;

import com.cloudcostai.entity.AnomalyDetectionConfig;
import com.cloudcostai.entity.AnomalySeverity;
import com.cloudcostai.entity.CloudAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ZScoreAnomalyDetectorTest {

    private ZScoreAnomalyDetector detector;
    private CloudAccount mockAccount;
    private AnomalyDetectionConfig defaultConfig;

    @BeforeEach
    void setUp() {
        detector = new ZScoreAnomalyDetector();
        mockAccount = CloudAccount.builder()
                .accountName("Production AWS")
                .build();
        mockAccount.setId(10L);

        defaultConfig = AnomalyDetectionConfig.builder()
                .zScoreThreshold(new BigDecimal("2.50"))
                .build();
    }

    @Test
    @DisplayName("Should detect no anomaly when spend is within normal historical variance")
    void shouldReturnEmptyWhenSpendIsNormal() {
        LocalDate today = LocalDate.of(2026, 11, 10);
        Map<LocalDate, BigDecimal> dailyCosts = new HashMap<>();
        dailyCosts.put(today.minusDays(5), new BigDecimal("100.00"));
        dailyCosts.put(today.minusDays(4), new BigDecimal("105.00"));
        dailyCosts.put(today.minusDays(3), new BigDecimal("98.00"));
        dailyCosts.put(today.minusDays(2), new BigDecimal("102.00"));
        dailyCosts.put(today.minusDays(1), new BigDecimal("101.00"));
        dailyCosts.put(today, new BigDecimal("104.00")); // normal spend

        List<AnomalyCandidate> anomalies = detector.detect(
                mockAccount, "Amazon EC2", "us-east-1", dailyCosts, today, defaultConfig);

        assertTrue(anomalies.isEmpty());
    }

    @Test
    @DisplayName("Should detect CRITICAL anomaly when spending surges over 4 sigma above mean")
    void shouldDetectCriticalAnomalyOnMassiveSpike() {
        LocalDate today = LocalDate.of(2026, 11, 10);
        Map<LocalDate, BigDecimal> dailyCosts = new HashMap<>();
        // Baseline around $100 with small variance
        dailyCosts.put(today.minusDays(6), new BigDecimal("100.00"));
        dailyCosts.put(today.minusDays(5), new BigDecimal("102.00"));
        dailyCosts.put(today.minusDays(4), new BigDecimal("99.00"));
        dailyCosts.put(today.minusDays(3), new BigDecimal("101.00"));
        dailyCosts.put(today.minusDays(2), new BigDecimal("100.00"));
        dailyCosts.put(today.minusDays(1), new BigDecimal("101.00"));
        dailyCosts.put(today, new BigDecimal("450.00")); // Massive 350% spike

        List<AnomalyCandidate> anomalies = detector.detect(
                mockAccount, "Amazon RDS", "us-east-1", dailyCosts, today, defaultConfig);

        assertFalse(anomalies.isEmpty());
        assertEquals(1, anomalies.size());

        AnomalyCandidate candidate = anomalies.get(0);
        assertEquals("Amazon RDS", candidate.getServiceName());
        assertEquals(AnomalySeverity.CRITICAL, candidate.getSeverity());
        assertTrue(candidate.getExcessCost().compareTo(new BigDecimal("340.00")) >= 0);
        assertTrue(candidate.getDeviationPercentage().compareTo(new BigDecimal("300.00")) >= 0);
    }

    @Test
    @DisplayName("Should handle zero standard deviation safely without division by zero")
    void shouldHandleZeroStandardDeviationSafely() {
        LocalDate today = LocalDate.of(2026, 11, 10);
        Map<LocalDate, BigDecimal> dailyCosts = new HashMap<>();
        // Exact flat spend of $100.00 on all prior days
        dailyCosts.put(today.minusDays(4), new BigDecimal("100.00"));
        dailyCosts.put(today.minusDays(3), new BigDecimal("100.00"));
        dailyCosts.put(today.minusDays(2), new BigDecimal("100.00"));
        dailyCosts.put(today.minusDays(1), new BigDecimal("100.00"));
        dailyCosts.put(today, new BigDecimal("300.00")); // Spike on flat baseline

        List<AnomalyCandidate> anomalies = assertDoesNotThrow(() ->
                detector.detect(mockAccount, "Amazon S3", "us-east-1", dailyCosts, today, defaultConfig));

        assertFalse(anomalies.isEmpty());
        assertEquals("Amazon S3", anomalies.get(0).getServiceName());
    }

    @Test
    @DisplayName("Should return empty list when historical data points are insufficient (< 4 days)")
    void shouldReturnEmptyWhenInsufficientHistoricalData() {
        LocalDate today = LocalDate.of(2026, 11, 10);
        Map<LocalDate, BigDecimal> dailyCosts = new HashMap<>();
        dailyCosts.put(today.minusDays(2), new BigDecimal("100.00"));
        dailyCosts.put(today.minusDays(1), new BigDecimal("105.00"));
        dailyCosts.put(today, new BigDecimal("500.00")); // Only 2 historical points

        List<AnomalyCandidate> anomalies = detector.detect(
                mockAccount, "Google Cloud Run", "us-central1", dailyCosts, today, defaultConfig);

        assertTrue(anomalies.isEmpty());
    }
}

