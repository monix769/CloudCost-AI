package com.cloudcostai.anomaly.algorithm;

import com.cloudcostai.entity.AnomalyDetectionConfig;
import com.cloudcostai.entity.AnomalySeverity;
import com.cloudcostai.entity.Budget;
import com.cloudcostai.entity.BudgetPeriod;
import com.cloudcostai.entity.CloudAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BudgetAnomalyDetectorTest {

    private BudgetAnomalyDetector detector;
    private CloudAccount mockAccount;
    private AnomalyDetectionConfig defaultConfig;

    @BeforeEach
    void setUp() {
        detector = new BudgetAnomalyDetector();
        mockAccount = CloudAccount.builder().accountName("Primary Account").build();
        mockAccount.setId(1L);

        defaultConfig = AnomalyDetectionConfig.builder()
                .budgetWarningThresholdPercent(new BigDecimal("90.00"))
                .budgetCriticalThresholdPercent(new BigDecimal("100.00"))
                .build();
    }

    @Test
    @DisplayName("Should detect CRITICAL anomaly when budget utilization reaches or exceeds 100%")
    void shouldDetectCriticalWhenBudgetExceeded() {
        Budget budget = Budget.builder()
                .name("Q4 Total Cloud Spend")
                .amountLimit(new BigDecimal("10000.00"))
                .currentSpend(new BigDecimal("10500.00")) // 105%
                .period(BudgetPeriod.QUARTERLY)
                .build();
        budget.setId(100L);

        List<AnomalyCandidate> candidates = detector.detectBudgetAnomaly(budget, mockAccount, defaultConfig);

        assertFalse(candidates.isEmpty());
        assertEquals(1, candidates.size());

        AnomalyCandidate candidate = candidates.get(0);
        assertEquals(AnomalySeverity.CRITICAL, candidate.getSeverity());
        assertEquals(new BigDecimal("500.0000"), candidate.getExcessCost());
        assertTrue(candidate.getRootCauseSummary().contains("Budget limit breach"));
    }

    @Test
    @DisplayName("Should detect HIGH anomaly when budget utilization reaches warning threshold (90-99%)")
    void shouldDetectHighWhenBudgetNearCapacity() {
        Budget budget = Budget.builder()
                .name("Dev Environment Monthly")
                .amountLimit(new BigDecimal("5000.00"))
                .currentSpend(new BigDecimal("4600.00")) // 92%
                .period(BudgetPeriod.MONTHLY)
                .build();
        budget.setId(101L);

        List<AnomalyCandidate> candidates = detector.detectBudgetAnomaly(budget, mockAccount, defaultConfig);

        assertFalse(candidates.isEmpty());
        assertEquals(1, candidates.size());

        AnomalyCandidate candidate = candidates.get(0);
        assertEquals(AnomalySeverity.HIGH, candidate.getSeverity());
        assertTrue(candidate.getRootCauseSummary().contains("Budget threshold warning"));
    }

    @Test
    @DisplayName("Should return empty when budget is operating well within healthy thresholds")
    void shouldReturnEmptyWhenHealthy() {
        Budget budget = Budget.builder()
                .name("Production S3 Storage")
                .amountLimit(new BigDecimal("20000.00"))
                .currentSpend(new BigDecimal("8500.00")) // 42.5%
                .period(BudgetPeriod.MONTHLY)
                .build();
        budget.setId(102L);

        List<AnomalyCandidate> candidates = detector.detectBudgetAnomaly(budget, mockAccount, defaultConfig);
        assertTrue(candidates.isEmpty());
    }
}
