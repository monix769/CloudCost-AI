package com.cloudcostai.cloud.scheduler;

import com.cloudcostai.cloud.lock.SyncLockManager;
import com.cloudcostai.cloud.service.BillingIngestionService;
import com.cloudcostai.entity.CloudAccount;
import com.cloudcostai.repository.CloudAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduled FinOps synchronization runner.
 * Automatically discovers connected cloud accounts across all tenant organizations
 * and synchronizes new billing data.
 *
 * Configurable via application properties:
 * - cloudcost.billing.sync.schedule-enabled: true/false
 * - cloudcost.billing.sync.fixed-delay-ms: milliseconds between runs (default: 1 hour)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BillingSyncScheduler {

    private final CloudAccountRepository cloudAccountRepository;
    private final BillingIngestionService billingIngestionService;
    private final SyncLockManager syncLockManager;

    @Value("${cloudcost.billing.sync.schedule-enabled:false}")
    private boolean scheduleEnabled;

    @Scheduled(fixedDelayString = "${cloudcost.billing.sync.fixed-delay-ms:3600000}", initialDelay = 60000)
    public void runScheduledSync() {
        if (!scheduleEnabled) {
            log.trace("Scheduled billing synchronization is disabled by configuration");
            return;
        }

        log.info("Starting scheduled multi-cloud billing synchronization run...");

        List<CloudAccount> connectedAccounts = cloudAccountRepository.findAll().stream()
                .filter(acc -> Boolean.TRUE.equals(acc.getIsActive()))
                .filter(acc -> "CONNECTED".equalsIgnoreCase(acc.getConnectionStatus()))
                .filter(acc -> acc.getEncryptedCredentials() != null && !acc.getEncryptedCredentials().trim().isEmpty())
                .toList();

        log.info("Discovered {} connected cloud accounts for scheduled billing sync", connectedAccounts.size());

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(14); // 14-day sliding ingestion window

        for (CloudAccount account : connectedAccounts) {
            if (syncLockManager.isLocked(account.getId())) {
                log.info("Skipping account [{}] - already locked by an active synchronization", account.getId());
                continue;
            }

            try {
                billingIngestionService.synchronizeAccountInternal(account, account.getOrganization(), startDate, endDate, false);
            } catch (Exception e) {
                log.error("Scheduled sync error for account [{}]: {}", account.getId(), e.getMessage());
            }
        }

        log.info("Scheduled multi-cloud billing synchronization completed");
    }
}
