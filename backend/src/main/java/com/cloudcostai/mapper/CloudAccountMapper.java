package com.cloudcostai.mapper;

import com.cloudcostai.dto.CloudAccountRequest;
import com.cloudcostai.dto.CloudAccountResponse;
import com.cloudcostai.entity.CloudAccount;
import com.cloudcostai.entity.Organization;
import org.springframework.stereotype.Component;

@Component
public class CloudAccountMapper {

    public CloudAccount toEntity(CloudAccountRequest request, Organization organization) {
        if (request == null) return null;
        return CloudAccount.builder()
                .organization(organization)
                .provider(request.getProvider())
                .accountId(request.getAccountId().trim())
                .accountName(request.getAccountName().trim())
                .arnOrClientId(request.getArnOrClientId() != null ? request.getArnOrClientId().trim() : null)
                .syncStatus("ACTIVE")
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
    }

    public CloudAccountResponse toResponse(CloudAccount entity) {
        if (entity == null) return null;
        return CloudAccountResponse.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganization() != null ? entity.getOrganization().getId() : null)
                .provider(entity.getProvider())
                .accountId(entity.getAccountId())
                .accountName(entity.getAccountName())
                .arnOrClientId(entity.getArnOrClientId())
                .syncStatus(entity.getSyncStatus())
                .connectionStatus(entity.getConnectionStatus() != null ? entity.getConnectionStatus() : "DISCONNECTED")
                .hasCredentialsConfigured(entity.getEncryptedCredentials() != null && !entity.getEncryptedCredentials().trim().isEmpty())
                .credentialsConfigured(entity.getEncryptedCredentials() != null && !entity.getEncryptedCredentials().trim().isEmpty())
                .keyVersion(entity.getKeyVersion())
                .lastSyncedAt(entity.getLastSyncedAt())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public void updateEntity(CloudAccount entity, CloudAccountRequest request) {
        if (entity == null || request == null) return;
        entity.setProvider(request.getProvider());
        entity.setAccountId(request.getAccountId().trim());
        entity.setAccountName(request.getAccountName().trim());
        entity.setArnOrClientId(request.getArnOrClientId() != null ? request.getArnOrClientId().trim() : null);
        if (request.getIsActive() != null) {
            entity.setIsActive(request.getIsActive());
        }
    }
}
