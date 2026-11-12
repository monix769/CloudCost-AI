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

class SpendingSpikeAnomalyDetectorTest {

    private SpendingSpikeAnomalyDetector detector;
    private CloudAccount mockAccount;
    private AnomalyDetectionConfig defaultConfig;

    @BeforeEach
    void setUp() {
        detector = new SpendingSpikeAnomalyDetector();
        mockAccount = CloudAccount.builder()
                .accountName("GCP Production")
                .build();
        mockAccount.setId(30L);

        defaultConfig = AnomalyDetectionConfig.builder()
                .spendingSpikePercent(new BigDecimal("50.00"))
                .spendingSpikeMinAmount(new BigDecimal("50.0000"))
                .build();
    }

    @Test
    @DisplayName("Should detect spending spike anomaly when daily cost doubles overnight")
    void shouldDetectSuddenOvernightSpendSpike() {
        LocalDate today = LocalDate.of(2026, 11, 10);
        Map<LocalDate, BigDecimal> dailyCosts = new HashMap<>();
        dailyCosts.put(today.minusDays(1), new BigDecimal("100.00"));
        dailyCosts.put(today, new BigDecimal("250.00")); // +150% spike ($150 excess)

        List<AnomalyCandidate> candidates = detector.detect(
                mockAccount, "Google Kubernetes Engine", "us-central1", dailyCosts, today, defaultConfig);

        assertFalse(candidates.isEmpty());
        assertEquals(1, candidates.size());

        AnomalyCandidate candidate = candidates.get(0);
        assertEquals(AnomalySeverity.HIGH, candidate.getSeverity());
        assertEquals(new BigDecimal("150.00"), candidate.getDeviationPercentage());
        assertEquals(new BigDecimal("150.0000"), candidate.getExcessCost());
    }

    @Test
    @DisplayName("Should ignore gradual day-over-day cost adjustments below spike threshold")
    void shouldIgnoreGradualDayOverDayAdjustments() {
        LocalDate today = LocalDate.of(2026, 11, 10);
        Map<LocalDate, BigDecimal> dailyCosts = new HashMap<>();
        dailyCosts.put(today.minusDays(1), new BigDecimal("100.00"));
        dailyCosts.put(today, new BigDecimal("120.00")); // +20% (below 50%)

        List<AnomalyCandidate> candidates = detector.detect(
                mockAccount, "Google Cloud Storage", "us-central1", dailyCosts, today, defaultConfig);

        assertTrue(candidates.isEmpty());
    }
}

