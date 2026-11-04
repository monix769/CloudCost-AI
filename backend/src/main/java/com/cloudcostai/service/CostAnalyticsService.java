package com.cloudcostai.service;

import com.cloudcostai.dto.CostSummaryResponse;
import com.cloudcostai.dto.CostTrendResponse;
import com.cloudcostai.dto.ProviderCostResponse;
import com.cloudcostai.dto.ServiceCostResponse;
import com.cloudcostai.entity.CloudProvider;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.repository.BillingRecordRepository;
import com.cloudcostai.repository.projection.MonthlyTrendSummary;
import com.cloudcostai.repository.projection.ProviderCostSummary;
import com.cloudcostai.repository.projection.ServiceCostSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CostAnalyticsService {

    private final BillingRecordRepository billingRecordRepository;
    private final OrganizationService organizationService;

    @Transactional(readOnly = true)
    public CostSummaryResponse getCostSummary(LocalDate startDate, LocalDate endDate) {
        Organization org = organizationService.getCurrentUserOrganization();

        BigDecimal totalCost = billingRecordRepository.calculateTotalCost(org.getId(), startDate, endDate);
        Long recordCount = billingRecordRepository.countRecords(org.getId(), startDate, endDate);

        if (totalCost == null) {
            totalCost = BigDecimal.ZERO;
        }

        BigDecimal avgDailyCost = BigDecimal.ZERO;
        if (startDate != null && endDate != null) {
            long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
            if (days > 0 && totalCost.compareTo(BigDecimal.ZERO) > 0) {
                avgDailyCost = totalCost.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);
            }
        } else if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            // Default 30-day window estimation if unconstrained
            avgDailyCost = totalCost.divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);
        }

        return CostSummaryResponse.builder()
                .totalCost(totalCost.setScale(2, RoundingMode.HALF_UP))
                .currency("USD")
                .startDate(startDate)
                .endDate(endDate)
                .totalRecords(recordCount != null ? recordCount : 0L)
                .averageDailyCost(avgDailyCost)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ProviderCostResponse> getCostsByProvider(LocalDate startDate, LocalDate endDate) {
        Organization org = organizationService.getCurrentUserOrganization();

        BigDecimal grandTotal = billingRecordRepository.calculateTotalCost(org.getId(), startDate, endDate);
        if (grandTotal == null || grandTotal.compareTo(BigDecimal.ZERO) == 0) {
            return List.of();
        }

        List<ProviderCostSummary> summaries = billingRecordRepository.aggregateCostByProvider(org.getId(), startDate, endDate);

        return summaries.stream().map(summary -> {
            BigDecimal providerTotal = summary.getTotalCost() != null ? summary.getTotalCost() : BigDecimal.ZERO;
            BigDecimal percentage = providerTotal.multiply(BigDecimal.valueOf(100))
                    .divide(grandTotal, 2, RoundingMode.HALF_UP);

            return ProviderCostResponse.builder()
                    .provider(summary.getProvider())
                    .totalCost(providerTotal.setScale(2, RoundingMode.HALF_UP))
                    .percentage(percentage)
                    .recordCount(summary.getRecordCount())
                    .build();
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceCostResponse> getCostsByService(LocalDate startDate, LocalDate endDate, Integer limit) {
        Organization org = organizationService.getCurrentUserOrganization();

        BigDecimal grandTotal = billingRecordRepository.calculateTotalCost(org.getId(), startDate, endDate);
        if (grandTotal == null || grandTotal.compareTo(BigDecimal.ZERO) == 0) {
            return List.of();
        }

        List<ServiceCostSummary> summaries = billingRecordRepository.aggregateCostByService(org.getId(), startDate, endDate);

        if (limit != null && limit > 0 && summaries.size() > limit) {
            summaries = summaries.subList(0, limit);
        }

        return summaries.stream().map(summary -> {
            BigDecimal serviceTotal = summary.getTotalCost() != null ? summary.getTotalCost() : BigDecimal.ZERO;
            BigDecimal percentage = serviceTotal.multiply(BigDecimal.valueOf(100))
                    .divide(grandTotal, 2, RoundingMode.HALF_UP);

            return ServiceCostResponse.builder()
                    .serviceName(summary.getServiceName())
                    .provider(summary.getProvider())
                    .totalCost(serviceTotal.setScale(2, RoundingMode.HALF_UP))
                    .percentage(percentage)
                    .recordCount(summary.getRecordCount())
                    .build();
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<CostTrendResponse> getCostTrends(LocalDate startDate, LocalDate endDate) {
        Organization org = organizationService.getCurrentUserOrganization();

        List<MonthlyTrendSummary> summaries = billingRecordRepository.aggregateMonthlyTrends(org.getId(), startDate, endDate);
        if (summaries == null || summaries.isEmpty()) {
            return List.of();
        }

        // Group rows by "YYYY-MM"
        Map<String, CostTrendResponse> trendMap = new LinkedHashMap<>();

        for (MonthlyTrendSummary row : summaries) {
            Integer year = row.getYear();
            Integer month = row.getMonth();
            if (year == null || month == null) continue;

            String periodKey = String.format("%04d-%02d", year, month);

            CostTrendResponse trend = trendMap.computeIfAbsent(periodKey, k -> CostTrendResponse.builder()
                    .period(periodKey)
                    .year(year)
                    .month(month)
                    .totalCost(BigDecimal.ZERO)
                    .awsCost(BigDecimal.ZERO)
                    .azureCost(BigDecimal.ZERO)
                    .gcpCost(BigDecimal.ZERO)
                    .build());

            BigDecimal cost = row.getTotalCost() != null ? row.getTotalCost() : BigDecimal.ZERO;
            trend.setTotalCost(trend.getTotalCost().add(cost));

            if (row.getProvider() == CloudProvider.AWS) {
                trend.setAwsCost(trend.getAwsCost().add(cost));
            } else if (row.getProvider() == CloudProvider.AZURE) {
                trend.setAzureCost(trend.getAzureCost().add(cost));
            } else if (row.getProvider() == CloudProvider.GCP) {
                trend.setGcpCost(trend.getGcpCost().add(cost));
            }
        }

        // Round all numbers to 2 decimal places
        for (CostTrendResponse trend : trendMap.values()) {
            trend.setTotalCost(trend.getTotalCost().setScale(2, RoundingMode.HALF_UP));
            trend.setAwsCost(trend.getAwsCost().setScale(2, RoundingMode.HALF_UP));
            trend.setAzureCost(trend.getAzureCost().setScale(2, RoundingMode.HALF_UP));
            trend.setGcpCost(trend.getGcpCost().setScale(2, RoundingMode.HALF_UP));
        }

        return new ArrayList<>(trendMap.values());
    }
}
