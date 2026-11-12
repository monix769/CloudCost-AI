package com.cloudcostai.anomaly.service;

import com.cloudcostai.anomaly.algorithm.BudgetAnomalyDetector;
import com.cloudcostai.anomaly.algorithm.MovingAverageAnomalyDetector;
import com.cloudcostai.anomaly.algorithm.SpendingSpikeAnomalyDetector;
import com.cloudcostai.anomaly.algorithm.ZScoreAnomalyDetector;
import com.cloudcostai.anomaly.lock.AnomalyDetectionLockManager;
import com.cloudcostai.dto.AnomalyDetectionResult;
import com.cloudcostai.entity.AnomalyDetectionConfig;
import com.cloudcostai.entity.AnomalySeverity;
import com.cloudcostai.entity.AnomalyStatus;
import com.cloudcostai.entity.BillingRecord;
import com.cloudcostai.entity.Budget;
import com.cloudcostai.entity.CloudAccount;
import com.cloudcostai.entity.CostAnomaly;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.mapper.CostAnomalyMapper;
import com.cloudcostai.repository.BillingRecordRepository;
import com.cloudcostai.repository.BudgetRepository;
import com.cloudcostai.repository.CloudAccountRepository;
import com.cloudcostai.repository.CostAnomalyHistoryRepository;
import com.cloudcostai.repository.CostAnomalyRepository;
import com.cloudcostai.repository.OrganizationRepository;
import com.cloudcostai.service.AnomalyConfigService;
import com.cloudcostai.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnomalyDetectionEngineServiceTest {

    @Mock
    private AnomalyDetectionLockManager lockManager;

    @Mock
    private CostAnomalyRepository costAnomalyRepository;

    @Mock
    private CostAnomalyHistoryRepository historyRepository;

    @Mock
    private CostAnomalyMapper costAnomalyMapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AnomalyConfigService anomalyConfigService;

    @Mock
    private BillingRecordRepository billingRecordRepository;

    @Mock
    private CloudAccountRepository cloudAccountRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ZScoreAnomalyDetector zScoreDetector;

    @Mock
    private MovingAverageAnomalyDetector movingAverageDetector;

    @Mock
    private SpendingSpikeAnomalyDetector spendingSpikeDetector;

    @Mock
    private BudgetAnomalyDetector budgetAnomalyDetector;

    @InjectMocks
    private AnomalyDetectionEngineService engineService;

    private Organization testOrg;
    private CloudAccount testAccount;
    private AnomalyDetectionConfig testConfig;

    @BeforeEach
    void setUp() {
        testOrg = Organization.builder().name("Acme Cloud Corp").build();
        testOrg.setId(1L);

        testAccount = CloudAccount.builder().accountName("Primary AWS").build();
        testAccount.setId(10L);

        testConfig = AnomalyDetectionConfig.builder().organization(testOrg).build();
    }

    @Test
    @DisplayName("Should abort execution early when lock cannot be acquired")
    void shouldAbortWhenLockCannotBeAcquired() {
        when(lockManager.tryLock(1L)).thenReturn(false);

        AnomalyDetectionResult result = engineService.runDetectionForOrganization(1L, LocalDate.now());

        assertNotNull(result);
        assertEquals(0, result.getCreatedCount());
        assertTrue(result.getSummaryMessage().contains("already active"));
        verify(organizationRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should detect new anomaly, persist record, record history, and dispatch notification")
    void shouldDetectAndPersistAnomaly() {
        when(lockManager.tryLock(1L)).thenReturn(true);
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrg));
        when(anomalyConfigService.getOrCreateEntity(testOrg)).thenReturn(testConfig);

        LocalDate today = LocalDate.of(2026, 11, 10);
        BillingRecord r1 = BillingRecord.builder()
                .cloudAccount(testAccount)
                .serviceName("Amazon EC2")
                .region("us-east-1")
                .billingPeriodStart(today)
                .costAmount(new BigDecimal("300.00"))
                .build();

        when(billingRecordRepository.findByOrganizationIdAndBillingPeriodStartBetweenOrderByBillingPeriodStartAsc(
                eq(1L), any(LocalDate.class), eq(today))).thenReturn(List.of(r1));

        // Mock Z-Score candidate
        com.cloudcostai.anomaly.algorithm.AnomalyCandidate candidate = com.cloudcostai.anomaly.algorithm.AnomalyCandidate.builder()
                .cloudAccount(testAccount)
                .serviceName("Amazon EC2")
                .region("us-east-1")
                .expectedCost(new BigDecimal("100.00"))
                .actualCost(new BigDecimal("300.00"))
                .excessCost(new BigDecimal("200.00"))
                .deviationPercentage(new BigDecimal("200.00"))
                .severity(AnomalySeverity.CRITICAL)
                .detectionMethod(com.cloudcostai.entity.AnomalyDetectionMethod.Z_SCORE)
                .anomalyPeriodStart(today)
                .anomalyPeriodEnd(today)
                .dimensionKey("account:10:service:amazon ec2:date:2026-11-10")
                .rootCauseSummary("Z-Score spike")
                .build();

        when(zScoreDetector.detect(any(), any(), any(), any(), eq(today), any()))
                .thenReturn(List.of(candidate));
        when(movingAverageDetector.detect(any(), any(), any(), any(), eq(today), any()))
                .thenReturn(Collections.emptyList());
        when(spendingSpikeDetector.detect(any(), any(), any(), any(), eq(today), any()))
                .thenReturn(Collections.emptyList());

        // Deduplication check: not already existing
        when(costAnomalyRepository.existsByOrganizationIdAndDimensionKeyAndAnomalyPeriodStartAndStatusIn(
                eq(1L), eq(candidate.getDimensionKey()), eq(today), anyCollection())).thenReturn(false);

        CostAnomaly savedAnomaly = CostAnomaly.builder()
                .organization(testOrg)
                .cloudAccount(testAccount)
                .serviceName("Amazon EC2")
                .severity(AnomalySeverity.CRITICAL)
                .build();
        savedAnomaly.setId(55L);

        when(costAnomalyRepository.save(any(CostAnomaly.class))).thenReturn(savedAnomaly);

        AnomalyDetectionResult result = engineService.runDetectionForOrganization(1L, today);

        assertNotNull(result);
        assertEquals(1, result.getCreatedCount());
        assertEquals(0, result.getSkippedCount());

        verify(historyRepository, times(1)).save(any());
        verify(notificationService, times(1)).createAnomalyNotification(savedAnomaly);
        verify(lockManager, times(1)).releaseLock(1L);
    }

    @Test
    @DisplayName("Should safely skip duplicate anomaly when identical open anomaly already exists")
    void shouldSafelySkipDuplicateAnomaly() {
        when(lockManager.tryLock(1L)).thenReturn(true);
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(testOrg));
        when(anomalyConfigService.getOrCreateEntity(testOrg)).thenReturn(testConfig);

        LocalDate today = LocalDate.of(2026, 11, 10);
        BillingRecord r1 = BillingRecord.builder()
                .cloudAccount(testAccount)
                .serviceName("Amazon RDS")
                .region("us-east-1")
                .billingPeriodStart(today)
                .costAmount(new BigDecimal("400.00"))
                .build();

        when(billingRecordRepository.findByOrganizationIdAndBillingPeriodStartBetweenOrderByBillingPeriodStartAsc(
                eq(1L), any(LocalDate.class), eq(today))).thenReturn(List.of(r1));

        com.cloudcostai.anomaly.algorithm.AnomalyCandidate candidate = com.cloudcostai.anomaly.algorithm.AnomalyCandidate.builder()
                .cloudAccount(testAccount)
                .serviceName("Amazon RDS")
                .region("us-east-1")
                .expectedCost(new BigDecimal("100.00"))
                .actualCost(new BigDecimal("400.00"))
                .excessCost(new BigDecimal("300.00"))
                .deviationPercentage(new BigDecimal("300.00"))
                .severity(AnomalySeverity.HIGH)
                .detectionMethod(com.cloudcostai.entity.AnomalyDetectionMethod.SPENDING_SPIKE)
                .anomalyPeriodStart(today)
                .anomalyPeriodEnd(today)
                .dimensionKey("account:10:service:amazon rds:date:2026-11-10")
                .build();

        when(spendingSpikeDetector.detect(any(), any(), any(), any(), eq(today), any()))
                .thenReturn(List.of(candidate));

        // Deduplication returns true: already active open anomaly exists
        when(costAnomalyRepository.existsByOrganizationIdAndDimensionKeyAndAnomalyPeriodStartAndStatusIn(
                eq(1L), eq(candidate.getDimensionKey()), eq(today), anyCollection())).thenReturn(true);

        AnomalyDetectionResult result = engineService.runDetectionForOrganization(1L, today);

        assertNotNull(result);
        assertEquals(0, result.getCreatedCount());
        assertEquals(1, result.getSkippedCount());

        verify(costAnomalyRepository, never()).save(any());
        verify(notificationService, never()).createAnomalyNotification(any());
        verify(lockManager, times(1)).releaseLock(1L);
    }
}

