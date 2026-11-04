package com.cloudcostai.mapper;

import com.cloudcostai.dto.BillingRecordRequest;
import com.cloudcostai.dto.BillingRecordResponse;
import com.cloudcostai.entity.BillingRecord;
import com.cloudcostai.entity.CloudAccount;
import com.cloudcostai.entity.Organization;
import org.springframework.stereotype.Component;

@Component
public class BillingRecordMapper {

    public BillingRecord toEntity(BillingRecordRequest request, Organization organization, CloudAccount account) {
        if (request == null) return null;
        return BillingRecord.builder()
                .organization(organization)
                .cloudAccount(account)
                .serviceName(request.getServiceName().trim())
                .region(request.getRegion().trim())
                .costAmount(request.getCostAmount())
                .currency(request.getCurrency() != null ? request.getCurrency().trim().toUpperCase() : "USD")
                .usageType(request.getUsageType() != null ? request.getUsageType().trim() : null)
                .usageQuantity(request.getUsageQuantity())
                .usageUnit(request.getUsageUnit() != null ? request.getUsageUnit().trim() : null)
                .billingPeriodStart(request.getBillingPeriodStart())
                .billingPeriodEnd(request.getBillingPeriodEnd())
                .resourceId(request.getResourceId() != null ? request.getResourceId().trim() : null)
                .tags(request.getTags())
                .sourceType(com.cloudcostai.entity.BillingSourceType.MANUAL)
                .build();
    }

    public BillingRecordResponse toResponse(BillingRecord entity) {
        if (entity == null) return null;
        return BillingRecordResponse.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganization() != null ? entity.getOrganization().getId() : null)
                .cloudAccountId(entity.getCloudAccount() != null ? entity.getCloudAccount().getId() : null)
                .provider(entity.getCloudAccount() != null ? entity.getCloudAccount().getProvider() : null)
                .accountName(entity.getCloudAccount() != null ? entity.getCloudAccount().getAccountName() : null)
                .serviceName(entity.getServiceName())
                .region(entity.getRegion())
                .costAmount(entity.getCostAmount())
                .currency(entity.getCurrency())
                .usageType(entity.getUsageType())
                .usageQuantity(entity.getUsageQuantity())
                .usageUnit(entity.getUsageUnit())
                .billingPeriodStart(entity.getBillingPeriodStart())
                .billingPeriodEnd(entity.getBillingPeriodEnd())
                .resourceId(entity.getResourceId())
                .tags(entity.getTags())
                .sourceType(entity.getSourceType() != null ? entity.getSourceType().name() : "MANUAL")
                .externalRecordId(entity.getExternalRecordId())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
