package com.cloudcostai.cloud.service;

import com.cloudcostai.cloud.provider.CloudBillingProvider;
import com.cloudcostai.cloud.provider.ConnectionValidationResult;
import com.cloudcostai.cloud.provider.ProviderCapabilities;
import com.cloudcostai.cloud.provider.ProviderCredentials;
import com.cloudcostai.dto.CloudAccountResponse;
import com.cloudcostai.entity.CloudAccount;
import com.cloudcostai.entity.CloudProvider;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.exception.BadRequestException;
import com.cloudcostai.exception.ResourceNotFoundException;
import com.cloudcostai.mapper.CloudAccountMapper;
import com.cloudcostai.repository.CloudAccountRepository;
import com.cloudcostai.security.CredentialEncryptionService;
import com.cloudcostai.service.OrganizationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudProviderService {

    private final List<CloudBillingProvider> providerList;
    private final CloudAccountRepository cloudAccountRepository;
    private final CloudAccountMapper cloudAccountMapper;
    private final CredentialEncryptionService credentialEncryptionService;
    private final OrganizationService organizationService;
    private final ObjectMapper objectMapper;

    private Map<CloudProvider, CloudBillingProvider> providers;

    @jakarta.annotation.PostConstruct
    public void init() {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(CloudBillingProvider::getProvider, Function.identity()));
    }

    public List<ProviderCapabilities> getAllCapabilities() {
        return providers.values().stream()
                .map(CloudBillingProvider::getCapabilities)
                .toList();
    }

    public ProviderCapabilities getCapabilities(CloudProvider provider) {
        CloudBillingProvider adapter = providers.get(provider);
        if (adapter == null) {
            throw new ResourceNotFoundException("CloudProvider", "provider", provider.name());
        }
        return adapter.getCapabilities();
    }

    /**
     * Ad-hoc validation of supplied credentials without modifying persisted account state.
     */
    public ConnectionValidationResult testCredentials(CloudProvider provider, ProviderCredentials credentials) {
        CloudBillingProvider adapter = providers.get(provider);
        if (adapter == null) {
            throw new ResourceNotFoundException("CloudProvider", "provider", provider.name());
        }
        CloudAccount probeAccount = CloudAccount.builder()
                .provider(provider)
                .accountId("probe-account")
                .accountName("Validation Probe")
                .build();
        ConnectionValidationResult result = adapter.validateConnection(probeAccount, credentials);
        result.setConnectionStatus(result.isValid() ? "CONNECTED" : "FAILED");
        return result;
    }

    /**
     * Validates connection for an already persisted cloud account using stored encrypted credentials.
     */
    @Transactional
    public ConnectionValidationResult validateAccountConnection(Long cloudAccountId) {
        Organization org = organizationService.getCurrentUserOrganization();
        CloudAccount account = cloudAccountRepository.findByIdAndOrganizationId(cloudAccountId, org.getId())
                .orElseThrow(() -> new ResourceNotFoundException("CloudAccount", "id", cloudAccountId));

        if (account.getEncryptedCredentials() == null || account.getEncryptedCredentials().trim().isEmpty()) {
            account.setConnectionStatus("DISCONNECTED");
            cloudAccountRepository.save(account);
            return ConnectionValidationResult.builder()
                    .valid(false)
                    .connectionStatus("DISCONNECTED")
                    .message("No credentials configured on this cloud account.")
                    .build();
        }

        try {
            String decrypted = credentialEncryptionService.decrypt(account.getEncryptedCredentials());
            ProviderCredentials credentials = objectMapper.readValue(decrypted, ProviderCredentials.class);

            CloudBillingProvider adapter = providers.get(account.getProvider());
            ConnectionValidationResult result = adapter.validateConnection(account, credentials);
            result.setConnectionStatus(result.isValid() ? "CONNECTED" : "FAILED");

            account.setConnectionStatus(result.getConnectionStatus());
            cloudAccountRepository.save(account);

            return result;
        } catch (Exception e) {
            log.error("Failed to validate account connection [ID: {}]: {}", cloudAccountId, e.getMessage());
            account.setConnectionStatus("FAILED");
            cloudAccountRepository.save(account);
            return ConnectionValidationResult.builder()
                    .valid(false)
                    .connectionStatus("FAILED")
                    .message("Validation error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Connects cloud account credentials.
     * Enforces Requirement 6: "Do not claim a provider is connected until actual credentials
     * and required permissions have been successfully validated."
     */
    @Transactional
    public CloudAccountResponse connectAccount(Long cloudAccountId, ProviderCredentials credentials) {
        Organization org = organizationService.getCurrentUserOrganization();
        CloudAccount account = cloudAccountRepository.findByIdAndOrganizationId(cloudAccountId, org.getId())
                .orElseThrow(() -> new ResourceNotFoundException("CloudAccount", "id", cloudAccountId));

        CloudBillingProvider adapter = providers.get(account.getProvider());
        if (adapter == null) {
            throw new IllegalStateException("Unsupported provider: " + account.getProvider());
        }

        // Validate credentials before marking as CONNECTED
        ConnectionValidationResult validation = adapter.validateConnection(account, credentials);
        if (!validation.isValid()) {
            account.setConnectionStatus("FAILED");
            cloudAccountRepository.save(account);
            throw new BadRequestException("Cloud credential validation failed: " + validation.getMessage());
        }

        try {
            // Encrypt with AES-256-GCM
            String credentialsJson = objectMapper.writeValueAsString(credentials);
            String encryptedPayload = credentialEncryptionService.encrypt(credentialsJson);

            account.setEncryptedCredentials(encryptedPayload);
            account.setConnectionStatus("CONNECTED");
            account.setKeyVersion(credentialEncryptionService.getActiveKeyVersion());

            CloudAccount saved = cloudAccountRepository.save(account);
            log.info("Cloud account [{}] successfully connected and credentials encrypted", saved.getId());
            return cloudAccountMapper.toResponse(saved);
        } catch (Exception e) {
            log.error("Failed to encrypt and store credentials for account [{}]: {}", cloudAccountId, e.getMessage());
            throw new RuntimeException("Failed to securely store cloud credentials: " + e.getMessage(), e);
        }
    }

    /**
     * Disconnects cloud account and completely purges stored encrypted credentials.
     */
    @Transactional
    public CloudAccountResponse disconnectAccount(Long cloudAccountId) {
        Organization org = organizationService.getCurrentUserOrganization();
        CloudAccount account = cloudAccountRepository.findByIdAndOrganizationId(cloudAccountId, org.getId())
                .orElseThrow(() -> new ResourceNotFoundException("CloudAccount", "id", cloudAccountId));

        account.setEncryptedCredentials(null);
        account.setConnectionStatus("DISCONNECTED");
        account.setSyncStatus("PENDING");

        CloudAccount saved = cloudAccountRepository.save(account);
        log.info("Cloud account [{}] disconnected and credentials purged", saved.getId());
        return cloudAccountMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public com.cloudcostai.dto.ConnectionStatusResponse getConnectionStatus(Long cloudAccountId) {
        Organization org = organizationService.getCurrentUserOrganization();
        CloudAccount account = cloudAccountRepository.findByIdAndOrganizationId(cloudAccountId, org.getId())
                .orElseThrow(() -> new ResourceNotFoundException("CloudAccount", "id", cloudAccountId));

        return com.cloudcostai.dto.ConnectionStatusResponse.builder()
                .cloudAccountId(account.getId())
                .provider(account.getProvider().name())
                .connectionStatus(account.getConnectionStatus() != null ? account.getConnectionStatus() : "DISCONNECTED")
                .syncStatus(account.getSyncStatus())
                .credentialsConfigured(account.getEncryptedCredentials() != null && !account.getEncryptedCredentials().trim().isEmpty())
                .keyVersion(account.getKeyVersion())
                .lastSyncedAt(account.getLastSyncedAt())
                .isSyncInProgress("SYNCING".equalsIgnoreCase(account.getSyncStatus()))
                .build();
    }
}
