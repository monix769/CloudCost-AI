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

class MovingAverageAnomalyDetectorTest {

    private MovingAverageAnomalyDetector detector;
    private CloudAccount mockAccount;
    private AnomalyDetectionConfig defaultConfig;

    @BeforeEach
    void setUp() {
        detector = new MovingAverageAnomalyDetector();
        mockAccount = CloudAccount.builder()
                .accountName("Azure Core")
                .build();
        mockAccount.setId(20L);

        defaultConfig = AnomalyDetectionConfig.builder()
                .movingAvgWindowDays(7)
                .movingAvgDeviationPercent(new BigDecimal("30.00"))
                .build();
    }

    @Test
    @DisplayName("Should detect moving average deviation anomaly when spend exceeds +30% threshold")
    void shouldDetectAnomalyWhenDeviationExceedsThreshold() {
        LocalDate today = LocalDate.of(2026, 11, 10);
        Map<LocalDate, BigDecimal> dailyCosts = new HashMap<>();
        // Average around $50
        for (int i = 7; i >= 1; i--) {
            dailyCosts.put(today.minusDays(i), new BigDecimal("50.00"));
        }
        dailyCosts.put(today, new BigDecimal("120.00")); // +140% deviation

        List<AnomalyCandidate> candidates = detector.detect(
                mockAccount, "Azure Virtual Machines", "eastus", dailyCosts, today, defaultConfig);

        assertFalse(candidates.isEmpty());
        assertEquals(1, candidates.size());

        AnomalyCandidate candidate = candidates.get(0);
        assertEquals(AnomalySeverity.HIGH, candidate.getSeverity());
        assertEquals(new BigDecimal("140.00"), candidate.getDeviationPercentage());
        assertEquals(new BigDecimal("70.0000"), candidate.getExcessCost());
    }

    @Test
    @DisplayName("Should return empty list when deviation is within threshold")
    void shouldReturnEmptyWhenWithinThreshold() {
        LocalDate today = LocalDate.of(2026, 11, 10);
        Map<LocalDate, BigDecimal> dailyCosts = new HashMap<>();
        for (int i = 7; i >= 1; i--) {
            dailyCosts.put(today.minusDays(i), new BigDecimal("100.00"));
        }
        dailyCosts.put(today, new BigDecimal("115.00")); // +15% deviation (under 30%)

        List<AnomalyCandidate> candidates = detector.detect(
                mockAccount, "Azure Cosmos DB", "eastus", dailyCosts, today, defaultConfig);

        assertTrue(candidates.isEmpty());
    }
}

