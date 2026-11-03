package com.cloudcostai.mapper;

import com.cloudcostai.dto.CostAnomalyResponse;
import com.cloudcostai.entity.CostAnomaly;
import org.springframework.stereotype.Component;

@Component
public class CostAnomalyMapper {

    public CostAnomalyResponse toResponse(CostAnomaly entity) {
        if (entity == null) return null;
        return CostAnomalyResponse.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganization() != null ? entity.getOrganization().getId() : null)
                .cloudAccountId(entity.getCloudAccount() != null ? entity.getCloudAccount().getId() : null)
                .provider(entity.getCloudAccount() != null ? entity.getCloudAccount().getProvider() : null)
                .accountName(entity.getCloudAccount() != null ? entity.getCloudAccount().getAccountName() : null)
                .serviceName(entity.getServiceName())
                .expectedCost(entity.getExpectedCost())
                .actualCost(entity.getActualCost())
                .deviationPercentage(entity.getDeviationPercentage())
                .severity(entity.getSeverity())
                .detectedAt(entity.getDetectedAt())
                .status(entity.getStatus())
                .rootCauseSummary(entity.getRootCauseSummary())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
