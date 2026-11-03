package com.cloudcostai.mapper;

import com.cloudcostai.dto.BudgetRequest;
import com.cloudcostai.dto.BudgetResponse;
import com.cloudcostai.entity.Budget;
import com.cloudcostai.entity.Organization;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class BudgetMapper {

    public Budget toEntity(BudgetRequest request, Organization organization) {
        if (request == null) return null;
        return Budget.builder()
                .organization(organization)
                .name(request.getName().trim())
                .amountLimit(request.getAmountLimit())
                .currentSpend(BigDecimal.ZERO)
                .currency(request.getCurrency() != null ? request.getCurrency().trim().toUpperCase() : "USD")
                .period(request.getPeriod())
                .alertThresholdPercentage(request.getAlertThresholdPercentage())
                .status(request.getStatus() != null ? request.getStatus().trim().toUpperCase() : "ACTIVE")
                .build();
    }

    public BudgetResponse toResponse(Budget entity, BigDecimal liveSpend) {
        if (entity == null) return null;

        BigDecimal spend = liveSpend != null ? liveSpend : (entity.getCurrentSpend() != null ? entity.getCurrentSpend() : BigDecimal.ZERO);
        BigDecimal limit = entity.getAmountLimit();

        BigDecimal remaining = limit.subtract(spend);

        BigDecimal utilization = BigDecimal.ZERO;
        if (limit != null && limit.compareTo(BigDecimal.ZERO) > 0) {
            utilization = spend.multiply(BigDecimal.valueOf(100))
                    .divide(limit, 2, RoundingMode.HALF_UP);
        }

        boolean isExceeded = spend.compareTo(limit) >= 0;
        boolean isNearThreshold = entity.getAlertThresholdPercentage() != null &&
                utilization.compareTo(entity.getAlertThresholdPercentage()) >= 0;

        return BudgetResponse.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganization() != null ? entity.getOrganization().getId() : null)
                .name(entity.getName())
                .amountLimit(limit)
                .currentSpend(spend)
                .utilizationPercentage(utilization)
                .remainingAmount(remaining)
                .currency(entity.getCurrency())
                .period(entity.getPeriod())
                .alertThresholdPercentage(entity.getAlertThresholdPercentage())
                .status(entity.getStatus())
                .isExceeded(isExceeded)
                .isNearThreshold(isNearThreshold)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public void updateEntity(Budget entity, BudgetRequest request) {
        if (entity == null || request == null) return;
        entity.setName(request.getName().trim());
        entity.setAmountLimit(request.getAmountLimit());
        if (request.getCurrency() != null) {
            entity.setCurrency(request.getCurrency().trim().toUpperCase());
        }
        if (request.getPeriod() != null) {
            entity.setPeriod(request.getPeriod());
        }
        if (request.getAlertThresholdPercentage() != null) {
            entity.setAlertThresholdPercentage(request.getAlertThresholdPercentage());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus().trim().toUpperCase());
        }
    }
}
