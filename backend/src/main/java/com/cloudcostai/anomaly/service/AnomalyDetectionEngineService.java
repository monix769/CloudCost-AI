package com.cloudcostai.anomaly.service;

import com.cloudcostai.anomaly.algorithm.AnomalyCandidate;
import com.cloudcostai.anomaly.algorithm.BudgetAnomalyDetector;
import com.cloudcostai.anomaly.algorithm.MovingAverageAnomalyDetector;
import com.cloudcostai.anomaly.algorithm.SpendingSpikeAnomalyDetector;
import com.cloudcostai.anomaly.algorithm.ZScoreAnomalyDetector;
import com.cloudcostai.anomaly.lock.AnomalyDetectionLockManager;
import com.cloudcostai.dto.AnomalyDetectionResult;
import com.cloudcostai.dto.CostAnomalyResponse;
import com.cloudcostai.entity.AnomalyDetectionConfig;
import com.cloudcostai.entity.AnomalyStatus;
import com.cloudcostai.entity.BillingRecord;
import com.cloudcostai.entity.Budget;
import com.cloudcostai.entity.CloudAccount;
import com.cloudcostai.entity.CostAnomaly;
import com.cloudcostai.entity.CostAnomalyHistory;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.exception.ResourceNotFoundException;
import com.cloudcostai.mapper.CostAnomalyMapper;
import com.cloudcostai.repository.BillingRecordRepository;
import com.cloudcostai.repository.BudgetRepository;
import com.cloudcostai.repository.CloudAccountRepository;
import com.cloudcostai.repository.CostAnomalyHistoryRepository;
import com.cloudcostai.repository.CostAnomalyRepository;
import com.cloudcostai.repository.OrganizationRepository;
import com.cloudcostai.service.AnomalyConfigService;
import com.cloudcostai.service.NotificationService;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyDetectionEngineService {

    private final AnomalyDetectionLockManager lockManager;
    private final CostAnomalyRepository costAnomalyRepository;
    private final CostAnomalyHistoryRepository historyRepository;
    private final CostAnomalyMapper costAnomalyMapper;
    private final NotificationService notificationService;
    private final AnomalyConfigService anomalyConfigService;
    private final BillingRecordRepository billingRecordRepository;
    private final CloudAccountRepository cloudAccountRepository;
    private final BudgetRepository budgetRepository;
    private final OrganizationRepository organizationRepository;

    private final ZScoreAnomalyDetector zScoreDetector;
    private final MovingAverageAnomalyDetector movingAverageDetector;
    private final SpendingSpikeAnomalyDetector spendingSpikeDetector;
    private final BudgetAnomalyDetector budgetAnomalyDetector;

    @Transactional
    public AnomalyDetectionResult runDetectionForOrganization(Long organizationId, LocalDate targetDate) {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }

        if (!lockManager.tryLock(organizationId)) {
            log.warn("Anomaly detection scan already in progress for organization [{}]. Skipping concurrent execution.", organizationId);
            return AnomalyDetectionResult.builder()
                    .evaluatedCount(0)
                    .createdCount(0)
                    .skippedCount(0)
                    .executionDurationMs(0)
                    .summaryMessage("Detection scan already active for this organization.")
                    .newlyDetectedAnomalies(Collections.emptyList())
                    .build();
        }

        long startTime = System.currentTimeMillis();
        LocalDate evalDate = targetDate != null ? targetDate : LocalDate.now();

        try {
            Organization org = organizationRepository.findById(organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));

            AnomalyDetectionConfig config = anomalyConfigService.getOrCreateEntity(org);

            log.info("Executing cost anomaly detection engine for organization [{}] on evaluation date [{}]...", organizationId, evalDate);

            // 1. Fetch billing records over a 45-day window ending on evalDate
            LocalDate windowStart = evalDate.minusDays(45);
            List<BillingRecord> records = billingRecordRepository.findByOrganizationIdAndBillingPeriodStartBetweenOrderByBillingPeriodStartAsc(
                    organizationId, windowStart, evalDate);

            // 2. Aggregate daily costs by WorkloadGroup: (CloudAccount, ServiceName, Region)
            Map<WorkloadKey, Map<LocalDate, BigDecimal>> workloadMap = new HashMap<>();
            for (BillingRecord record : records) {
                if (record.getCloudAccount() == null || record.getServiceName() == null) continue;

                WorkloadKey key = new WorkloadKey(
                        record.getCloudAccount(),
                        record.getServiceName().trim(),
                        record.getRegion() != null ? record.getRegion().trim() : "GLOBAL"
                );

                LocalDate recordDate = record.getBillingPeriodStart();
                BigDecimal cost = record.getCostAmount() != null ? record.getCostAmount() : BigDecimal.ZERO;

                workloadMap.computeIfAbsent(key, k -> new TreeMap<>())
                        .merge(recordDate, cost, BigDecimal::add);
            }

            List<AnomalyCandidate> candidates = new ArrayList<>();

            // 3. Evaluate each workload across time-series statistical detectors
            for (Map.Entry<WorkloadKey, Map<LocalDate, BigDecimal>> entry : workloadMap.entrySet()) {
                WorkloadKey key = entry.getKey();
                Map<LocalDate, BigDecimal> dailyCosts = entry.getValue();

                // Run Z-Score Detection
                candidates.addAll(zScoreDetector.detect(key.getAccount(), key.getServiceName(), key.getRegion(), dailyCosts, evalDate, config));

                // Run Moving Average Deviation
                candidates.addAll(movingAverageDetector.detect(key.getAccount(), key.getServiceName(), key.getRegion(), dailyCosts, evalDate, config));

                // Run Spending Spike Detection
                candidates.addAll(spendingSpikeDetector.detect(key.getAccount(), key.getServiceName(), key.getRegion(), dailyCosts, evalDate, config));
            }

            // 4. Evaluate Budgets
            List<Budget> budgets = budgetRepository.findByOrganizationId(organizationId);
            List<CloudAccount> accounts = cloudAccountRepository.findByOrganizationId(organizationId);
            CloudAccount defaultAccount = accounts.isEmpty() ? null : accounts.get(0);

            if (defaultAccount != null) {
                for (Budget budget : budgets) {
                    candidates.addAll(budgetAnomalyDetector.detectBudgetAnomaly(budget, defaultAccount, config));
                }
            }

            int evaluatedCount = candidates.size();
            int createdCount = 0;
            int skippedCount = 0;
            List<CostAnomalyResponse> createdResponses = new ArrayList<>();

            // 5. Deduplicate and Persist
            for (AnomalyCandidate candidate : candidates) {
                // Safeguard against duplicate anomaly alerts for the same resource dimension & window
                boolean alreadyOpen = costAnomalyRepository.existsByOrganizationIdAndDimensionKeyAndAnomalyPeriodStartAndStatusIn(
                        organizationId,
                        candidate.getDimensionKey(),
                        candidate.getAnomalyPeriodStart(),
                        List.of(AnomalyStatus.OPEN, AnomalyStatus.ACKNOWLEDGED)
                );

                if (alreadyOpen) {
                    skippedCount++;
                    log.debug("Skipped duplicate active anomaly for dimensionKey [{}] in org [{}]", candidate.getDimensionKey(), organizationId);
                    continue;
                }

                CostAnomaly entity = CostAnomaly.builder()
                        .organization(org)
                        .cloudAccount(candidate.getCloudAccount())
                        .serviceName(candidate.getServiceName())
                        .region(candidate.getRegion())
                        .expectedCost(candidate.getExpectedCost())
                        .actualCost(candidate.getActualCost())
                        .excessCost(candidate.getExcessCost())
                        .deviationPercentage(candidate.getDeviationPercentage())
                        .severity(candidate.getSeverity())
                        .detectionMethod(candidate.getDetectionMethod())
                        .detectedAt(Instant.now())
                        .anomalyPeriodStart(candidate.getAnomalyPeriodStart())
                        .anomalyPeriodEnd(candidate.getAnomalyPeriodEnd())
                        .dimensionKey(candidate.getDimensionKey())
                        .status(AnomalyStatus.OPEN)
                        .rootCauseSummary(candidate.getRootCauseSummary())
                        .recommendedAction(candidate.getRecommendedAction())
                        .build();

                CostAnomaly saved = costAnomalyRepository.save(entity);

                // Record initial state transition in history audit log
                CostAnomalyHistory initialHistory = CostAnomalyHistory.builder()
                        .costAnomaly(saved)
                        .previousStatus(null)
                        .newStatus(AnomalyStatus.OPEN)
                        .actionByUserId(null)
                        .actionByUserEmail("SYSTEM (Anomaly Engine)")
                        .notes("Identified via " + candidate.getDetectionMethod().getDisplayName() + ".")
                        .createdAt(Instant.now())
                        .build();
                historyRepository.save(initialHistory);

                // Dispatch in-app notification for CRITICAL and HIGH severity anomalies
                notificationService.createAnomalyNotification(saved);

                createdResponses.add(costAnomalyMapper.toResponse(saved));
                createdCount++;
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Anomaly detection completed for org [{}] in {} ms: {} candidates evaluated, {} created, {} duplicates skipped.",
                    organizationId, duration, evaluatedCount, createdCount, skippedCount);

            return AnomalyDetectionResult.builder()
                    .evaluatedCount(evaluatedCount)
                    .createdCount(createdCount)
                    .skippedCount(skippedCount)
                    .executionDurationMs(duration)
                    .summaryMessage(String.format("Detection complete: %d anomalies flagged and alerted, %d active duplicates skipped.",
                            createdCount, skippedCount))
                    .newlyDetectedAnomalies(createdResponses)
                    .build();

        } finally {
            lockManager.releaseLock(organizationId);
        }
    }

    @Getter
    @AllArgsConstructor
    @EqualsAndHashCode
    private static class WorkloadKey {
        private final CloudAccount account;
        private final String serviceName;
        private final String region;
    }
}

