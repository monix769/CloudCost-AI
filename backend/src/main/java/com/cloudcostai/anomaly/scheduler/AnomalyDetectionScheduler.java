package com.cloudcostai.anomaly.scheduler;

import com.cloudcostai.anomaly.service.AnomalyDetectionEngineService;
import com.cloudcostai.dto.AnomalyDetectionResult;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Background runner for automated scheduled FinOps anomaly detection.
 * Periodically scans new billing data across all active tenant organizations
 * and flags statistical anomalies.
 *
 * Configurable via:
 * - cloudcost.anomaly.schedule.enabled: true/false (default: true)
 * - cloudcost.anomaly.schedule.fixed-delay-ms: milliseconds between scans (default: 1 hour)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnomalyDetectionScheduler {

    private final OrganizationRepository organizationRepository;
    private final AnomalyDetectionEngineService detectionEngineService;

    @Value("${cloudcost.anomaly.schedule.enabled:true}")
    private boolean scheduleEnabled;

    @Scheduled(
            fixedDelayString = "${cloudcost.anomaly.schedule.fixed-delay-ms:3600000}",
            initialDelay = 120000
    )
    public void runScheduledAnomalyDetection() {
        if (!scheduleEnabled) {
            log.trace("Scheduled anomaly detection is disabled via cloudcost.anomaly.schedule.enabled=false");
            return;
        }

        log.info("Starting scheduled multi-tenant cost anomaly detection sweep...");

        List<Organization> activeOrgs = organizationRepository.findAll().stream()
                .filter(org -> Boolean.TRUE.equals(org.getIsActive()))
                .toList();

        log.info("Discovered {} active tenant organizations for anomaly detection", activeOrgs.size());
        LocalDate today = LocalDate.now();

        for (Organization org : activeOrgs) {
            try {
                AnomalyDetectionResult result = detectionEngineService.runDetectionForOrganization(org.getId(), today);
                log.info("Scheduled detection completed for org [{} (ID: {})]: {} created, {} skipped in {} ms",
                        org.getName(), org.getId(), result.getCreatedCount(), result.getSkippedCount(), result.getExecutionDurationMs());
            } catch (Exception ex) {
                log.error("Error running scheduled anomaly detection for org [{}]", org.getId(), ex);
            }
        }

        log.info("Scheduled anomaly detection sweep completed.");
    }
}

