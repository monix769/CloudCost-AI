package com.cloudcostai.optimization.engine;

import com.cloudcostai.entity.OptimizationPriority;
import com.cloudcostai.entity.OptimizationRecommendation;
import com.cloudcostai.entity.OptimizationStatus;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.optimization.model.CandidateRecommendation;
import com.cloudcostai.repository.OptimizationRecommendationRepository;
import com.cloudcostai.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class OptimizationEngineService {

    private final RightsizingEngineService rightsizingEngineService;
    private final IdleResourceEngineService idleResourceEngineService;
    private final CommitmentAnalysisEngineService commitmentAnalysisEngineService;
    private final OptimizationRecommendationRepository recommendationRepository;
    private final NotificationService notificationService;

    // Per-organization execution lock to avoid concurrent race conditions
    private final Map<Long, Boolean> executionLocks = new ConcurrentHashMap<>();

    private static final List<OptimizationStatus> ACTIVE_STATUSES = Arrays.asList(
            OptimizationStatus.NEW,
            OptimizationStatus.REVIEWED,
            OptimizationStatus.APPROVED
    );

    @Transactional
    public Map<String, Object> runOptimizationAnalysis(Organization org) {
        if (org == null) {
            return Map.of("status", "FAILED", "reason", "Organization is null");
        }

        Long orgId = org.getId();
        if (executionLocks.putIfAbsent(orgId, Boolean.TRUE) != null) {
            log.warn("Optimization analysis already in progress for organization [{}]", orgId);
            return Map.of(
                    "status", "ALREADY_RUNNING",
                    "message", "An optimization scan is currently running for this organization."
            );
        }

        long startTime = System.currentTimeMillis();
        int candidatesTotal = 0;
        int createdCount = 0;
        int updatedCount = 0;

        try {
            log.info("Starting automated cost optimization & rightsizing engine for org [{}]...", orgId);

            List<CandidateRecommendation> allCandidates = new ArrayList<>();

            // 1. Evaluate Rightsizing Opportunities
            try {
                allCandidates.addAll(rightsizingEngineService.evaluate(org));
            } catch (Exception e) {
                log.error("Error running rightsizing engine for org [{}]: {}", orgId, e.getMessage(), e);
            }

            // 2. Evaluate Idle Resources
            try {
                allCandidates.addAll(idleResourceEngineService.evaluate(org));
            } catch (Exception e) {
                log.error("Error running idle resource engine for org [{}]: {}", orgId, e.getMessage(), e);
            }

            // 3. Evaluate Baseline Commitments (Savings Plans / RIs)
            try {
                allCandidates.addAll(commitmentAnalysisEngineService.evaluate(org));
            } catch (Exception e) {
                log.error("Error running commitment analysis for org [{}]: {}", orgId, e.getMessage(), e);
            }

            candidatesTotal = allCandidates.size();

            // 4. Deduplicate and Persist
            for (CandidateRecommendation candidate : allCandidates) {
                Long accountId = candidate.getCloudAccount() != null ? candidate.getCloudAccount().getId() : null;

                Optional<OptimizationRecommendation> existingOpt = Optional.empty();
                if (accountId != null) {
                    existingOpt = recommendationRepository.findTopByOrganizationIdAndCloudAccountIdAndRecommendationTypeAndResourceIdAndStatusIn(
                            orgId, accountId, candidate.getRecommendationType(), candidate.getResourceId(), ACTIVE_STATUSES);
                }

                if (existingOpt.isPresent()) {
                    // Update existing recommendation metrics
                    OptimizationRecommendation existing = existingOpt.get();
                    existing.setCurrentMonthlyCost(candidate.getCurrentMonthlyCost());
                    existing.setRecommendedMonthlyCost(candidate.getRecommendedMonthlyCost());
                    existing.setEstimatedMonthlySavings(candidate.getEstimatedMonthlySavings());
                    existing.setEstimatedAnnualSavings(candidate.getEstimatedAnnualSavings());
                    existing.setReason(candidate.getReason());
                    existing.setSuggestedConfig(candidate.getSuggestedConfig());
                    existing.setPriority(candidate.getPriority());
                    existing.setConfidenceLevel(candidate.getConfidenceLevel());
                    existing.setUpdatedAt(Instant.now());
                    recommendationRepository.save(existing);
                    updatedCount++;
                } else {
                    // Create new recommendation
                    OptimizationRecommendation newRec = OptimizationRecommendation.builder()
                            .organization(org)
                            .cloudAccount(candidate.getCloudAccount())
                            .resourceId(candidate.getResourceId())
                            .resourceName(candidate.getResourceName())
                            .resourceType(candidate.getResourceType())
                            .cloudProvider(candidate.getCloudProvider())
                            .recommendationType(candidate.getRecommendationType())
                            .currentConfig(candidate.getCurrentConfig())
                            .suggestedConfig(candidate.getSuggestedConfig())
                            .priority(candidate.getPriority())
                            .status(OptimizationStatus.NEW)
                            .confidenceLevel(candidate.getConfidenceLevel())
                            .reason(candidate.getReason())
                            .currentMonthlyCost(candidate.getCurrentMonthlyCost())
                            .recommendedMonthlyCost(candidate.getRecommendedMonthlyCost())
                            .estimatedMonthlySavings(candidate.getEstimatedMonthlySavings())
                            .estimatedAnnualSavings(candidate.getEstimatedAnnualSavings())
                            .currency(candidate.getCurrency() != null ? candidate.getCurrency() : "USD")
                            .detectedAt(Instant.now())
                            .build();

                    OptimizationRecommendation savedRec = recommendationRepository.save(newRec);
                    createdCount++;

                    // Send notification if CRITICAL or HIGH priority
                    if (savedRec.getPriority() == OptimizationPriority.CRITICAL || savedRec.getPriority() == OptimizationPriority.HIGH) {
                        try {
                            notificationService.createOptimizationNotification(savedRec);
                        } catch (Exception ex) {
                            log.warn("Failed to create in-app notification for optimization [ID: {}]: {}", savedRec.getId(), ex.getMessage());
                        }
                    }
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Optimization engine completed for org [{}] in {}ms: {} candidates, {} created, {} updated",
                    orgId, elapsed, candidatesTotal, createdCount, updatedCount);

            return Map.of(
                    "status", "SUCCESS",
                    "executionTimeMs", elapsed,
                    "candidatesEvaluated", candidatesTotal,
                    "newRecommendationsCreated", createdCount,
                    "existingRecommendationsUpdated", updatedCount
            );
        } finally {
            executionLocks.remove(orgId);
        }
    }
}

