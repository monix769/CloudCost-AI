package com.cloudcostai.cloud.service;

import com.cloudcostai.cloud.lock.SyncLockManager;
import com.cloudcostai.cloud.provider.CloudBillingProvider;
import com.cloudcostai.cloud.provider.NormalizedBillingRecord;
import com.cloudcostai.cloud.provider.ProviderCredentials;
import com.cloudcostai.cloud.util.BillingFingerprintGenerator;
import com.cloudcostai.entity.BillingRecord;
import com.cloudcostai.entity.CloudAccount;
import com.cloudcostai.entity.CloudProvider;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.entity.SyncHistory;
import com.cloudcostai.exception.BadRequestException;
import com.cloudcostai.exception.ConflictException;
import com.cloudcostai.exception.ResourceNotFoundException;
import com.cloudcostai.repository.BillingRecordRepository;
import com.cloudcostai.repository.CloudAccountRepository;
import com.cloudcostai.repository.SyncHistoryRepository;
import com.cloudcostai.security.CredentialEncryptionService;
import com.cloudcostai.service.OrganizationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingIngestionService {

    private final CloudAccountRepository cloudAccountRepository;
    private final BillingRecordRepository billingRecordRepository;
    private final SyncHistoryRepository syncHistoryRepository;
    private final List<CloudBillingProvider> providerList;
    private final CredentialEncryptionService credentialEncryptionService;
    private final SyncLockManager syncLockManager;
    private final OrganizationService organizationService;
    private final ObjectMapper objectMapper;

    private Map<CloudProvider, CloudBillingProvider> providers;

    @jakarta.annotation.PostConstruct
    public void init() {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(CloudBillingProvider::getProvider, Function.identity()));
        log.info("BillingIngestionService registered {} cloud providers: {}", providers.size(), providers.keySet());
    }

    /**
     * Executes cloud billing ingestion for a target cloud account.
     * Enforces tenant isolation, concurrency locking, bounded retries, and database-level duplicate prevention.
     */
    public SyncHistory synchronizeAccount(Long cloudAccountId, LocalDate startDate, LocalDate endDate, boolean isManual) {
        Organization org = organizationService.getCurrentUserOrganization();

        CloudAccount account = cloudAccountRepository.findByIdAndOrganizationId(cloudAccountId, org.getId())
                .orElseThrow(() -> new ResourceNotFoundException("CloudAccount", "id", cloudAccountId));

        return synchronizeAccountInternal(account, org, startDate, endDate, isManual);
    }

    public SyncHistory synchronizeAccountInternal(CloudAccount account, Organization org, LocalDate startDate, LocalDate endDate, boolean isManual) {
        Long cloudAccountId = account.getId();

        if (!Boolean.TRUE.equals(account.getIsActive())) {
            throw new BadRequestException("Cannot synchronize an inactive cloud account");
        }

        if (account.getEncryptedCredentials() == null || account.getEncryptedCredentials().trim().isEmpty()) {
            throw new BadRequestException("Cloud account is not connected. Please configure credentials before synchronizing.");
        }

        // Prevent overlapping runs (Requirement 3)
        if (!syncLockManager.tryLock(cloudAccountId)) {
            throw new ConflictException("A synchronization job is already running for cloud account: " + account.getAccountName());
        }

        LocalDate start = (startDate != null) ? startDate : LocalDate.now().minusDays(30);
        LocalDate end = (endDate != null) ? endDate : LocalDate.now();

        if (start.isAfter(end)) {
            syncLockManager.releaseLock(cloudAccountId);
            throw new BadRequestException("Start date cannot be after end date");
        }

        // Record initial IN_PROGRESS sync history entry
        SyncHistory syncHistory = SyncHistory.builder()
                .organization(org)
                .cloudAccount(account)
                .provider(account.getProvider().name())
                .syncStartTime(Instant.now())
                .syncStatus("IN_PROGRESS")
                .recordsFetched(0)
                .recordsInserted(0)
                .recordsSkipped(0)
                .dateRangeStart(start)
                .dateRangeEnd(end)
                .build();
        syncHistory = syncHistoryRepository.save(syncHistory);

        account.setSyncStatus("SYNCING");
        cloudAccountRepository.save(account);

        try {
            // Decrypt credentials
            String decryptedCredsJson = credentialEncryptionService.decrypt(account.getEncryptedCredentials());
            ProviderCredentials credentials = objectMapper.readValue(decryptedCredsJson, ProviderCredentials.class);

            CloudBillingProvider providerAdapter = providers.get(account.getProvider());
            if (providerAdapter == null) {
                throw new IllegalStateException("No provider adapter found for: " + account.getProvider());
            }

            // Bounded retry with exponential backoff for transient provider throttling/network errors
            List<NormalizedBillingRecord> fetchedRecords = fetchWithRetry(providerAdapter, account, credentials, start, end);

            int insertedCount = 0;
            int skippedCount = 0;

            for (NormalizedBillingRecord normRec : fetchedRecords) {
                // Compute deterministic idempotency fingerprint (Requirement 1)
                String fingerprint = BillingFingerprintGenerator.computeFingerprint(
                        org.getId(), account.getId(), account.getProvider(), normRec);
                normRec.setExternalRecordId(fingerprint);

                // Initial repository check
                if (billingRecordRepository.existsByOrganizationIdAndCloudAccountIdAndExternalRecordId(
                        org.getId(), account.getId(), fingerprint)) {
                    skippedCount++;
                    continue;
                }

                // Prepare BillingRecord entity
                BillingRecord entity = BillingRecord.builder()
                        .organization(org)
                        .cloudAccount(account)
                        .serviceName(normRec.getServiceName())
                        .region(normRec.getRegion() != null ? normRec.getRegion() : "global")
                        .costAmount(normRec.getCostAmount())
                        .currency(normRec.getCurrency() != null ? normRec.getCurrency() : "USD")
                        .usageType(normRec.getUsageType())
                        .usageQuantity(normRec.getUsageQuantity())
                        .usageUnit(normRec.getUsageUnit())
                        .billingPeriodStart(normRec.getBillingPeriodStart())
                        .billingPeriodEnd(normRec.getBillingPeriodEnd())
                        .resourceId(normRec.getResourceId())
                        .tags(normRec.getTags())
                        .sourceType(normRec.getSourceType())
                        .externalRecordId(fingerprint)
                        .build();

                // Database-level duplicate prevention handling (Requirement 2)
                try {
                    billingRecordRepository.save(entity);
                    insertedCount++;
                } catch (DataIntegrityViolationException ex) {
                    log.debug("Database unique constraint caught concurrent duplicate insert for fingerprint: {}", fingerprint);
                    skippedCount++;
                }
            }

            // Update Account state
            account.setSyncStatus("SYNCED");
            account.setConnectionStatus("CONNECTED");
            account.setLastSyncedAt(Instant.now());
            cloudAccountRepository.save(account);

            // Update SyncHistory state
            syncHistory.setSyncStatus("SUCCESS");
            syncHistory.setRecordsFetched(fetchedRecords.size());
            syncHistory.setRecordsInserted(insertedCount);
            syncHistory.setRecordsSkipped(skippedCount);
            syncHistory.setSyncEndTime(Instant.now());
            syncHistory = syncHistoryRepository.save(syncHistory);

            log.info("Billing sync complete for account [{}] [Fetched: {}, Inserted: {}, Skipped: {}]",
                    account.getId(), fetchedRecords.size(), insertedCount, skippedCount);

            return syncHistory;
        } catch (Exception e) {
            log.error("Billing synchronization failed for account [{}]: {}", account.getId(), e.getMessage(), e);

            account.setSyncStatus("FAILED");
            cloudAccountRepository.save(account);

            syncHistory.setSyncStatus("FAILED");
            syncHistory.setSyncEndTime(Instant.now());
            syncHistory.setErrorSummary(e.getMessage() != null ? e.getMessage() : "Unknown synchronization error");
            syncHistory = syncHistoryRepository.save(syncHistory);

            throw new RuntimeException("Billing synchronization failed: " + e.getMessage(), e);
        } finally {
            syncLockManager.releaseLock(cloudAccountId);
        }
    }

    private List<NormalizedBillingRecord> fetchWithRetry(
            CloudBillingProvider provider,
            CloudAccount account,
            ProviderCredentials credentials,
            LocalDate start,
            LocalDate end) {

        int maxAttempts = 3;
        long backoffMs = 500;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return provider.fetchBillingData(account, credentials, start, end);
            } catch (BadRequestException e) {
                // Do not retry validation or non-transient errors
                throw e;
            } catch (Exception e) {
                if (attempt == maxAttempts) {
                    throw new RuntimeException("Failed after " + maxAttempts + " attempts: " + e.getMessage(), e);
                }
                log.warn("Provider fetch attempt {} failed ({}), retrying in {}ms...", attempt, e.getMessage(), backoffMs);
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Sync interrupted during backoff", ie);
                }
                backoffMs *= 2; // Exponential backoff
            }
        }
        return List.of();
    }

    @Transactional(readOnly = true)
    public Page<SyncHistory> getSyncHistory(Long cloudAccountId, Pageable pageable) {
        Organization org = organizationService.getCurrentUserOrganization();
        if (cloudAccountId != null) {
            // Verify ownership
            cloudAccountRepository.findByIdAndOrganizationId(cloudAccountId, org.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("CloudAccount", "id", cloudAccountId));
            return syncHistoryRepository.findByOrganizationIdAndCloudAccountId(org.getId(), cloudAccountId, pageable);
        }
        return syncHistoryRepository.findByOrganizationId(org.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public SyncHistory getSyncHistoryById(Long syncId) {
        Organization org = organizationService.getCurrentUserOrganization();
        return syncHistoryRepository.findByIdAndOrganizationId(syncId, org.getId())
                .orElseThrow(() -> new ResourceNotFoundException("SyncHistory", "id", syncId));
    }

    @Transactional(readOnly = true)
    public Optional<SyncHistory> getLatestSyncStatus(Long cloudAccountId) {
        Organization org = organizationService.getCurrentUserOrganization();
        cloudAccountRepository.findByIdAndOrganizationId(cloudAccountId, org.getId())
                .orElseThrow(() -> new ResourceNotFoundException("CloudAccount", "id", cloudAccountId));
        return syncHistoryRepository.findTopByOrganizationIdAndCloudAccountIdOrderBySyncStartTimeDesc(org.getId(), cloudAccountId);
    }
}
