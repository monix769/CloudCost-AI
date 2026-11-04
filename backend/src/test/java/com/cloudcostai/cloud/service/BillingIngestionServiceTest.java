package com.cloudcostai.cloud.service;

import com.cloudcostai.cloud.lock.SyncLockManager;
import com.cloudcostai.cloud.provider.CloudBillingProvider;
import com.cloudcostai.cloud.provider.NormalizedBillingRecord;
import com.cloudcostai.cloud.provider.ProviderCredentials;
import com.cloudcostai.entity.BillingRecord;
import com.cloudcostai.entity.BillingSourceType;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingIngestionServiceTest {

    @Mock
    private CloudAccountRepository cloudAccountRepository;
    @Mock
    private BillingRecordRepository billingRecordRepository;
    @Mock
    private SyncHistoryRepository syncHistoryRepository;
    @Mock
    private CloudBillingProvider awsProvider;
    @Mock
    private CredentialEncryptionService credentialEncryptionService;
    @Mock
    private SyncLockManager syncLockManager;
    @Mock
    private OrganizationService organizationService;

    private ObjectMapper objectMapper;
    private BillingIngestionService ingestionService;

    private Organization testOrg;
    private CloudAccount testAccount;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        when(awsProvider.getProvider()).thenReturn(CloudProvider.AWS);

        ingestionService = new BillingIngestionService(
                cloudAccountRepository,
                billingRecordRepository,
                syncHistoryRepository,
                List.of(awsProvider),
                credentialEncryptionService,
                syncLockManager,
                organizationService,
                objectMapper
        );
        ingestionService.init();

        testOrg = Organization.builder()
                .name("Acme Corp")
                .slug("acme-corp")
                .build();
        testOrg.setId(10L);

        testAccount = CloudAccount.builder()
                .organization(testOrg)
                .provider(CloudProvider.AWS)
                .accountId("123456789012")
                .accountName("Production AWS")
                .encryptedCredentials("v1:encryptedBlob")
                .isActive(true)
                .connectionStatus("CONNECTED")
                .build();
        testAccount.setId(100L);
    }

    @Test
    @DisplayName("Should successfully synchronize billing data and record sync history")
    void shouldSuccessfullySynchronizeBillingData() throws Exception {
        when(organizationService.getCurrentUserOrganization()).thenReturn(testOrg);
        when(cloudAccountRepository.findByIdAndOrganizationId(100L, 10L)).thenReturn(Optional.of(testAccount));
        when(syncLockManager.tryLock(100L)).thenReturn(true);

        ProviderCredentials credentials = ProviderCredentials.builder()
                .awsAccessKeyId("AKIAIOSFODNN7EXAMPLE")
                .awsSecretAccessKey("secret")
                .build();
        when(credentialEncryptionService.decrypt("v1:encryptedBlob")).thenReturn(objectMapper.writeValueAsString(credentials));

        NormalizedBillingRecord rec1 = NormalizedBillingRecord.builder()
                .provider(CloudProvider.AWS)
                .serviceName("Amazon EC2")
                .costAmount(new BigDecimal("50.00"))
                .currency("USD")
                .billingPeriodStart(LocalDate.of(2026, 5, 1))
                .billingPeriodEnd(LocalDate.of(2026, 5, 1))
                .sourceType(BillingSourceType.REAL_PROVIDER)
                .build();

        when(awsProvider.fetchBillingData(eq(testAccount), any(ProviderCredentials.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(rec1));

        when(syncHistoryRepository.save(any(SyncHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        SyncHistory history = ingestionService.synchronizeAccount(100L, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), true);

        assertThat(history.getSyncStatus()).isEqualTo("SUCCESS");
        assertThat(history.getRecordsFetched()).isEqualTo(1);
        assertThat(history.getRecordsInserted()).isEqualTo(1);
        assertThat(history.getRecordsSkipped()).isZero();

        verify(billingRecordRepository).save(any(BillingRecord.class));
        verify(syncLockManager).releaseLock(100L);
    }

    @Test
    @DisplayName("Should catch database DataIntegrityViolationException and increment skipped count")
    void shouldCatchDatabaseDuplicateConstraintViolation() throws Exception {
        when(organizationService.getCurrentUserOrganization()).thenReturn(testOrg);
        when(cloudAccountRepository.findByIdAndOrganizationId(100L, 10L)).thenReturn(Optional.of(testAccount));
        when(syncLockManager.tryLock(100L)).thenReturn(true);

        ProviderCredentials credentials = ProviderCredentials.builder().awsAccessKeyId("key").build();
        when(credentialEncryptionService.decrypt("v1:encryptedBlob")).thenReturn(objectMapper.writeValueAsString(credentials));

        NormalizedBillingRecord rec1 = NormalizedBillingRecord.builder()
                .provider(CloudProvider.AWS)
                .serviceName("Amazon EC2")
                .costAmount(new BigDecimal("50.00"))
                .billingPeriodStart(LocalDate.of(2026, 5, 1))
                .billingPeriodEnd(LocalDate.of(2026, 5, 1))
                .build();

        when(awsProvider.fetchBillingData(any(), any(), any(), any())).thenReturn(List.of(rec1));
        when(syncHistoryRepository.save(any(SyncHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        // Simulate concurrent duplicate insert triggering MySQL unique constraint uq_billing_record_dedup
        when(billingRecordRepository.save(any(BillingRecord.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry 'fp:abc123' for key 'uq_billing_record_dedup'"));

        SyncHistory history = ingestionService.synchronizeAccount(100L, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), true);

        assertThat(history.getSyncStatus()).isEqualTo("SUCCESS");
        assertThat(history.getRecordsFetched()).isEqualTo(1);
        assertThat(history.getRecordsInserted()).isZero();
        assertThat(history.getRecordsSkipped()).isEqualTo(1);

        verify(syncLockManager).releaseLock(100L);
    }

    @Test
    @DisplayName("Should throw ConflictException when sync lock cannot be acquired")
    void shouldThrowConflictWhenAccountIsAlreadyLocked() {
        when(organizationService.getCurrentUserOrganization()).thenReturn(testOrg);
        when(cloudAccountRepository.findByIdAndOrganizationId(100L, 10L)).thenReturn(Optional.of(testAccount));
        when(syncLockManager.tryLock(100L)).thenReturn(false);

        assertThatThrownBy(() -> ingestionService.synchronizeAccount(100L, null, null, true))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already running");

        verify(syncHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when accessing another organization's cloud account")
    void shouldPreventCrossTenantSyncAccess() {
        Organization otherOrg = Organization.builder().name("Evil Corp").build();
        otherOrg.setId(999L);
        when(organizationService.getCurrentUserOrganization()).thenReturn(otherOrg);
        when(cloudAccountRepository.findByIdAndOrganizationId(100L, 999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ingestionService.synchronizeAccount(100L, null, null, true))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("CloudAccount not found");

        verify(syncLockManager, never()).tryLock(any());
    }
}
