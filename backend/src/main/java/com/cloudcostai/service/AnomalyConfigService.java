package com.cloudcostai.service;

import com.cloudcostai.dto.AnomalyConfigRequest;
import com.cloudcostai.dto.AnomalyConfigResponse;
import com.cloudcostai.entity.AnomalyDetectionConfig;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.repository.AnomalyDetectionConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyConfigService {

    private final AnomalyDetectionConfigRepository configRepository;
    private final OrganizationService organizationService;

    @Transactional(readOnly = true)
    public AnomalyConfigResponse getConfig() {
        Organization org = organizationService.getCurrentUserOrganization();
        AnomalyDetectionConfig config = getOrCreateEntity(org);
        return toResponse(config);
    }

    @Transactional
    public AnomalyConfigResponse updateConfig(AnomalyConfigRequest request) {
        Organization org = organizationService.getCurrentUserOrganization();
        AnomalyDetectionConfig config = getOrCreateEntity(org);

        if (request.getZScoreThreshold() != null) {
            config.setZScoreThreshold(request.getZScoreThreshold());
        }
        if (request.getMovingAvgWindowDays() != null) {
            config.setMovingAvgWindowDays(request.getMovingAvgWindowDays());
        }
        if (request.getMovingAvgDeviationPercent() != null) {
            config.setMovingAvgDeviationPercent(request.getMovingAvgDeviationPercent());
        }
        if (request.getSpendingSpikePercent() != null) {
            config.setSpendingSpikePercent(request.getSpendingSpikePercent());
        }
        if (request.getSpendingSpikeMinAmount() != null) {
            config.setSpendingSpikeMinAmount(request.getSpendingSpikeMinAmount());
        }
        if (request.getBudgetWarningThresholdPercent() != null) {
            config.setBudgetWarningThresholdPercent(request.getBudgetWarningThresholdPercent());
        }
        if (request.getBudgetCriticalThresholdPercent() != null) {
            config.setBudgetCriticalThresholdPercent(request.getBudgetCriticalThresholdPercent());
        }
        if (request.getIsAutomatedDetectionEnabled() != null) {
            config.setIsAutomatedDetectionEnabled(request.getIsAutomatedDetectionEnabled());
        }

        config.setUpdatedAt(Instant.now());
        AnomalyDetectionConfig updated = configRepository.save(config);
        log.info("Updated anomaly detection configuration for org [{}]", org.getId());
        return toResponse(updated);
    }

    @Transactional
    public AnomalyDetectionConfig getOrCreateEntity(Organization org) {
        return configRepository.findByOrganizationId(org.getId())
                .orElseGet(() -> {
                    AnomalyDetectionConfig defaultConfig = AnomalyDetectionConfig.builder()
                            .organization(org)
                            .zScoreThreshold(new BigDecimal("2.50"))
                            .movingAvgWindowDays(14)
                            .movingAvgDeviationPercent(new BigDecimal("30.00"))
                            .spendingSpikePercent(new BigDecimal("50.00"))
                            .spendingSpikeMinAmount(new BigDecimal("50.0000"))
                            .budgetWarningThresholdPercent(new BigDecimal("90.00"))
                            .budgetCriticalThresholdPercent(new BigDecimal("100.00"))
                            .isAutomatedDetectionEnabled(true)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();
                    return configRepository.save(defaultConfig);
                });
    }

    public AnomalyConfigResponse toResponse(AnomalyDetectionConfig config) {
        if (config == null) return null;
        return AnomalyConfigResponse.builder()
                .organizationId(config.getOrganization() != null ? config.getOrganization().getId() : null)
                .zScoreThreshold(config.getZScoreThreshold())
                .movingAvgWindowDays(config.getMovingAvgWindowDays())
                .movingAvgDeviationPercent(config.getMovingAvgDeviationPercent())
                .spendingSpikePercent(config.getSpendingSpikePercent())
                .spendingSpikeMinAmount(config.getSpendingSpikeMinAmount())
                .budgetWarningThresholdPercent(config.getBudgetWarningThresholdPercent())
                .budgetCriticalThresholdPercent(config.getBudgetCriticalThresholdPercent())
                .isAutomatedDetectionEnabled(config.getIsAutomatedDetectionEnabled())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}

