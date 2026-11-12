package com.cloudcostai.service;

import com.cloudcostai.dto.AccountCostResponse;
import com.cloudcostai.dto.AnalyticsOverviewResponse;
import com.cloudcostai.dto.CostAllocationDimension;
import com.cloudcostai.dto.CostAllocationItemResponse;
import com.cloudcostai.dto.CostAllocationResponse;
import com.cloudcostai.dto.CostSummaryResponse;
import com.cloudcostai.dto.CostTrendResponse;
import com.cloudcostai.dto.MonthOverMonthResponse;
import com.cloudcostai.dto.ProviderCostResponse;
import com.cloudcostai.dto.ProviderMoMComparison;
import com.cloudcostai.dto.ServiceCostResponse;
import com.cloudcostai.dto.ServiceMoMComparison;
import com.cloudcostai.dto.SpendingTrendPointResponse;
import com.cloudcostai.dto.SpendingTrendsSeriesResponse;
import com.cloudcostai.dto.TimeGranularity;
import com.cloudcostai.entity.CloudAccount;
import com.cloudcostai.entity.CloudProvider;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.exception.BadRequestException;
import com.cloudcostai.repository.BillingRecordRepository;
import com.cloudcostai.repository.CloudAccountRepository;
import com.cloudcostai.repository.projection.AccountCostSummary;
import com.cloudcostai.repository.projection.DailyCostSummary;
import com.cloudcostai.repository.projection.MonthlyTrendSummary;
import com.cloudcostai.repository.projection.ProviderCostSummary;
import com.cloudcostai.repository.projection.RegionCostSummary;
import com.cloudcostai.repository.projection.ServiceCostSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CostAnalyticsService {

    private final BillingRecordRepository billingRecordRepository;
    private final CloudAccountRepository cloudAccountRepository;
    private final OrganizationService organizationService;
    private final BudgetService budgetService;

    // Backward-compatible 2-parameter method (M3)
    @Transactional(readOnly = true)
    public CostSummaryResponse getCostSummary(LocalDate startDate, LocalDate endDate) {
        return getCostSummary(startDate, endDate, null);
    }

    @Transactional(readOnly = true)
    public CostSummaryResponse getCostSummary(LocalDate startDate, LocalDate endDate, String requestedCurrency) {
        Organization org = organizationService.getCurrentUserOrganization();
        String activeCurrency = resolveAndValidateCurrency(org.getId(), requestedCurrency);

        BigDecimal totalCost = billingRecordRepository.calculateTotalCost(org.getId(), activeCurrency, startDate, endDate);
        Long recordCount = billingRecordRepository.countRecords(org.getId(), activeCurrency, startDate, endDate);

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
            avgDailyCost = totalCost.divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);
        }

        // Current & Previous calendar month expenditure
        LocalDate now = LocalDate.now();
        LocalDate curMonthStart = now.withDayOfMonth(1);
        LocalDate curMonthEnd = now.with(TemporalAdjusters.lastDayOfMonth());

        LocalDate prevMonthDate = now.minusMonths(1);
        LocalDate prevMonthStart = prevMonthDate.withDayOfMonth(1);
        LocalDate prevMonthEnd = prevMonthDate.with(TemporalAdjusters.lastDayOfMonth());

        BigDecimal currentMonthCost = billingRecordRepository.calculateTotalCost(org.getId(), activeCurrency, curMonthStart, curMonthEnd);
        BigDecimal previousMonthCost = billingRecordRepository.calculateTotalCost(org.getId(), activeCurrency, prevMonthStart, prevMonthEnd);

        // Peak and Lowest spending days
        List<DailyCostSummary> dailyList = billingRecordRepository.aggregateDailyCosts(org.getId(), activeCurrency, startDate, endDate);
        LocalDate highestSpendingDate = null;
        BigDecimal highestSpendingDayCost = BigDecimal.ZERO;
        LocalDate lowestSpendingDate = null;
        BigDecimal lowestSpendingDayCost = null;

        if (dailyList != null && !dailyList.isEmpty()) {
            for (DailyCostSummary day : dailyList) {
                BigDecimal cost = day.getTotalCost() != null ? day.getTotalCost() : BigDecimal.ZERO;
                if (cost.compareTo(highestSpendingDayCost) > 0 || highestSpendingDate == null) {
                    highestSpendingDayCost = cost;
                    highestSpendingDate = day.getUsageDate();
                }
                if (lowestSpendingDayCost == null || cost.compareTo(lowestSpendingDayCost) < 0) {
                    lowestSpendingDayCost = cost;
                    lowestSpendingDate = day.getUsageDate();
                }
            }
        }

        return CostSummaryResponse.builder()
                .totalCost(totalCost.setScale(2, RoundingMode.HALF_UP))
                .currency(activeCurrency)
                .startDate(startDate)
                .endDate(endDate)
                .totalRecords(recordCount != null ? recordCount : 0L)
                .averageDailyCost(avgDailyCost)
                .currentMonthCost(currentMonthCost != null ? currentMonthCost.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .previousMonthCost(previousMonthCost != null ? previousMonthCost.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .highestSpendingDate(highestSpendingDate)
                .highestSpendingDayCost(highestSpendingDayCost.setScale(2, RoundingMode.HALF_UP))
                .lowestSpendingDate(lowestSpendingDate)
                .lowestSpendingDayCost(lowestSpendingDayCost != null ? lowestSpendingDayCost.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .build();
    }

    // Backward-compatible 2-parameter method (M3)
    @Transactional(readOnly = true)
    public List<ProviderCostResponse> getCostsByProvider(LocalDate startDate, LocalDate endDate) {
        return getCostsByProvider(startDate, endDate, null);
    }

    @Transactional(readOnly = true)
    public List<ProviderCostResponse> getCostsByProvider(LocalDate startDate, LocalDate endDate, String requestedCurrency) {
        Organization org = organizationService.getCurrentUserOrganization();
        String activeCurrency = resolveAndValidateCurrency(org.getId(), requestedCurrency);

        BigDecimal grandTotal = billingRecordRepository.calculateTotalCost(org.getId(), activeCurrency, startDate, endDate);
        if (grandTotal == null || grandTotal.compareTo(BigDecimal.ZERO) == 0) {
            return List.of();
        }

        List<ProviderCostSummary> summaries = billingRecordRepository.aggregateCostByProvider(org.getId(), activeCurrency, startDate, endDate);

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

    // Backward-compatible 3-parameter method (M3)
    @Transactional(readOnly = true)
    public List<ServiceCostResponse> getCostsByService(LocalDate startDate, LocalDate endDate, Integer limit) {
        return getCostsByService(startDate, endDate, limit, null);
    }

    @Transactional(readOnly = true)
    public List<ServiceCostResponse> getCostsByService(LocalDate startDate, LocalDate endDate, Integer limit, String requestedCurrency) {
        Organization org = organizationService.getCurrentUserOrganization();
        String activeCurrency = resolveAndValidateCurrency(org.getId(), requestedCurrency);

        BigDecimal grandTotal = billingRecordRepository.calculateTotalCost(org.getId(), activeCurrency, startDate, endDate);
        if (grandTotal == null || grandTotal.compareTo(BigDecimal.ZERO) == 0) {
            return List.of();
        }

        List<ServiceCostSummary> summaries = billingRecordRepository.aggregateCostByService(org.getId(), activeCurrency, startDate, endDate);

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
    public List<ServiceCostResponse> getTopSpendingServices(Integer limit, String requestedCurrency) {
        int effectiveLimit = (limit != null && limit > 0) ? limit : 5;
        return getCostsByService(null, null, effectiveLimit, requestedCurrency);
    }

    @Transactional(readOnly = true)
    public List<AccountCostResponse> getCostsByAccount(LocalDate startDate, LocalDate endDate, Integer limit, String requestedCurrency) {
        Organization org = organizationService.getCurrentUserOrganization();
        String activeCurrency = resolveAndValidateCurrency(org.getId(), requestedCurrency);

        BigDecimal grandTotal = billingRecordRepository.calculateTotalCost(org.getId(), activeCurrency, startDate, endDate);
        if (grandTotal == null || grandTotal.compareTo(BigDecimal.ZERO) == 0) {
            return List.of();
        }

        List<AccountCostSummary> summaries = billingRecordRepository.aggregateCostByAccount(org.getId(), activeCurrency, startDate, endDate);
        if (limit != null && limit > 0 && summaries.size() > limit) {
            summaries = summaries.subList(0, limit);
        }

        Map<Long, CloudAccount> accountMap = cloudAccountRepository.findByOrganizationId(org.getId()).stream()
                .collect(Collectors.toMap(CloudAccount::getId, a -> a, (k1, k2) -> k1));

        return summaries.stream().map(summary -> {
            BigDecimal accountTotal = summary.getTotalCost() != null ? summary.getTotalCost() : BigDecimal.ZERO;
            BigDecimal percentage = accountTotal.multiply(BigDecimal.valueOf(100))
                    .divide(grandTotal, 2, RoundingMode.HALF_UP);

            CloudAccount account = accountMap.get(summary.getAccountId());
            String accountNumber = (account != null && account.getAccountId() != null)
                    ? account.getAccountId()
                    : String.valueOf(summary.getAccountId());

            return AccountCostResponse.builder()
                    .accountId(summary.getAccountId())
                    .accountNumber(accountNumber)
                    .accountName(summary.getAccountName())
                    .provider(summary.getProvider())
                    .totalCost(accountTotal.setScale(2, RoundingMode.HALF_UP))
                    .percentage(percentage)
                    .recordCount(summary.getRecordCount())
                    .build();
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<AccountCostResponse> getTopSpendingAccounts(Integer limit, String requestedCurrency) {
        int effectiveLimit = (limit != null && limit > 0) ? limit : 5;
        return getCostsByAccount(null, null, effectiveLimit, requestedCurrency);
    }

    // Backward-compatible method (M3)
    @Transactional(readOnly = true)
    public List<CostTrendResponse> getCostTrends(LocalDate startDate, LocalDate endDate) {
        Organization org = organizationService.getCurrentUserOrganization();
        String activeCurrency = resolveAndValidateCurrency(org.getId(), null);

        List<MonthlyTrendSummary> summaries = billingRecordRepository.aggregateMonthlyTrends(org.getId(), activeCurrency, startDate, endDate);
        if (summaries == null || summaries.isEmpty()) {
            return List.of();
        }

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

        for (CostTrendResponse trend : trendMap.values()) {
            trend.setTotalCost(trend.getTotalCost().setScale(2, RoundingMode.HALF_UP));
            trend.setAwsCost(trend.getAwsCost().setScale(2, RoundingMode.HALF_UP));
            trend.setAzureCost(trend.getAzureCost().setScale(2, RoundingMode.HALF_UP));
            trend.setGcpCost(trend.getGcpCost().setScale(2, RoundingMode.HALF_UP));
        }

        return new ArrayList<>(trendMap.values());
    }

    @Transactional(readOnly = true)
    public SpendingTrendsSeriesResponse getSpendingTrends(
            TimeGranularity granularity,
            LocalDate startDate,
            LocalDate endDate,
            String requestedCurrency,
            CloudProvider providerFilter) {

        Organization org = organizationService.getCurrentUserOrganization();
        String activeCurrency = resolveAndValidateCurrency(org.getId(), requestedCurrency);
        TimeGranularity activeGranularity = (granularity != null) ? granularity : TimeGranularity.MONTHLY;

        List<DailyCostSummary> dailyRecords = (providerFilter != null)
                ? billingRecordRepository.aggregateDailyCostsByProvider(org.getId(), activeCurrency, providerFilter, startDate, endDate)
                : billingRecordRepository.aggregateDailyCosts(org.getId(), activeCurrency, startDate, endDate);

        if (dailyRecords == null || dailyRecords.isEmpty()) {
            return SpendingTrendsSeriesResponse.builder()
                    .granularity(activeGranularity)
                    .currency(activeCurrency)
                    .startDate(startDate)
                    .endDate(endDate)
                    .totalSpend(BigDecimal.ZERO)
                    .averageSpend(BigDecimal.ZERO)
                    .minSpend(BigDecimal.ZERO)
                    .maxSpend(BigDecimal.ZERO)
                    .points(new ArrayList<>())
                    .build();
        }

        // Aggregate daily entries into requested granularity buckets
        Map<String, List<DailyCostSummary>> bucketMap = new LinkedHashMap<>();
        for (DailyCostSummary day : dailyRecords) {
            if (day.getUsageDate() == null) continue;
            String key = formatBucketKey(day.getUsageDate(), activeGranularity);
            bucketMap.computeIfAbsent(key, k -> new ArrayList<>()).add(day);
        }

        List<SpendingTrendPointResponse> points = new ArrayList<>();
        BigDecimal seriesTotal = BigDecimal.ZERO;
        BigDecimal previousBucketTotal = null;

        BigDecimal minBucketSpend = null;
        BigDecimal maxBucketSpend = BigDecimal.ZERO;

        for (Map.Entry<String, List<DailyCostSummary>> entry : bucketMap.entrySet()) {
            String bucketKey = entry.getKey();
            List<DailyCostSummary> items = entry.getValue();

            BigDecimal bucketTotal = items.stream()
                    .map(d -> d.getTotalCost() != null ? d.getTotalCost() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long bucketRecords = items.stream()
                    .mapToLong(d -> d.getRecordCount() != null ? d.getRecordCount() : 0L)
                    .sum();

            LocalDate bStart = items.stream().map(DailyCostSummary::getUsageDate).min(Comparator.naturalOrder()).orElse(null);
            LocalDate bEnd = items.stream().map(DailyCostSummary::getUsageDate).max(Comparator.naturalOrder()).orElse(null);

            BigDecimal minDay = items.stream().map(d -> d.getTotalCost() != null ? d.getTotalCost() : BigDecimal.ZERO).min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
            BigDecimal maxDay = items.stream().map(d -> d.getTotalCost() != null ? d.getTotalCost() : BigDecimal.ZERO).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);

            BigDecimal avgDaily = (items.size() > 0)
                    ? bucketTotal.divide(BigDecimal.valueOf(items.size()), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal pctChange = BigDecimal.ZERO;
            if (previousBucketTotal != null && previousBucketTotal.compareTo(BigDecimal.ZERO) > 0) {
                pctChange = bucketTotal.subtract(previousBucketTotal)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(previousBucketTotal, 2, RoundingMode.HALF_UP);
            }

            points.add(SpendingTrendPointResponse.builder()
                    .period(bucketKey)
                    .startDate(bStart)
                    .endDate(bEnd)
                    .totalCost(bucketTotal.setScale(2, RoundingMode.HALF_UP))
                    .averageDailyCost(avgDaily)
                    .minCost(minDay.setScale(2, RoundingMode.HALF_UP))
                    .maxCost(maxDay.setScale(2, RoundingMode.HALF_UP))
                    .percentageChange(pctChange)
                    .previousPeriodCost(previousBucketTotal != null ? previousBucketTotal.setScale(2, RoundingMode.HALF_UP) : null)
                    .recordCount(bucketRecords)
                    .build());

            seriesTotal = seriesTotal.add(bucketTotal);

            if (minBucketSpend == null || bucketTotal.compareTo(minBucketSpend) < 0) {
                minBucketSpend = bucketTotal;
            }
            if (bucketTotal.compareTo(maxBucketSpend) > 0) {
                maxBucketSpend = bucketTotal;
            }

            previousBucketTotal = bucketTotal;
        }

        BigDecimal seriesAvg = (points.size() > 0)
                ? seriesTotal.divide(BigDecimal.valueOf(points.size()), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return SpendingTrendsSeriesResponse.builder()
                .granularity(activeGranularity)
                .currency(activeCurrency)
                .startDate(startDate)
                .endDate(endDate)
                .totalSpend(seriesTotal.setScale(2, RoundingMode.HALF_UP))
                .averageSpend(seriesAvg)
                .minSpend(minBucketSpend != null ? minBucketSpend.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .maxSpend(maxBucketSpend.setScale(2, RoundingMode.HALF_UP))
                .points(points)
                .build();
    }

    private String formatBucketKey(LocalDate date, TimeGranularity granularity) {
        return switch (granularity) {
            case DAILY -> date.toString();
            case WEEKLY -> String.format("%d-W%02d", date.get(IsoFields.WEEK_BASED_YEAR), date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
            case MONTHLY -> String.format("%04d-%02d", date.getYear(), date.getMonthValue());
            case QUARTERLY -> String.format("%04d-Q%d", date.getYear(), (date.getMonthValue() - 1) / 3 + 1);
            case YEARLY -> String.format("%04d", date.getYear());
        };
    }

    @Transactional(readOnly = true)
    public MonthOverMonthResponse getMonthOverMonthComparison(String requestedCurrency) {
        Organization org = organizationService.getCurrentUserOrganization();
        String activeCurrency = resolveAndValidateCurrency(org.getId(), requestedCurrency);

        LocalDate now = LocalDate.now();
        LocalDate curMonthStart = now.withDayOfMonth(1);
        LocalDate curMonthEnd = now.with(TemporalAdjusters.lastDayOfMonth());

        LocalDate prevMonthDate = now.minusMonths(1);
        LocalDate prevMonthStart = prevMonthDate.withDayOfMonth(1);
        LocalDate prevMonthEnd = prevMonthDate.with(TemporalAdjusters.lastDayOfMonth());

        String curKey = String.format("%04d-%02d", curMonthStart.getYear(), curMonthStart.getMonthValue());
        String prevKey = String.format("%04d-%02d", prevMonthStart.getYear(), prevMonthStart.getMonthValue());

        BigDecimal currentCost = billingRecordRepository.calculateTotalCost(org.getId(), activeCurrency, curMonthStart, curMonthEnd);
        BigDecimal previousCost = billingRecordRepository.calculateTotalCost(org.getId(), activeCurrency, prevMonthStart, prevMonthEnd);

        if (currentCost == null) currentCost = BigDecimal.ZERO;
        if (previousCost == null) previousCost = BigDecimal.ZERO;

        BigDecimal delta = currentCost.subtract(previousCost);
        BigDecimal pctChange = BigDecimal.ZERO;
        if (previousCost.compareTo(BigDecimal.ZERO) > 0) {
            pctChange = delta.multiply(BigDecimal.valueOf(100)).divide(previousCost, 2, RoundingMode.HALF_UP);
        } else if (currentCost.compareTo(BigDecimal.ZERO) > 0) {
            pctChange = new BigDecimal("100.00");
        }

        String direction = delta.compareTo(BigDecimal.ZERO) > 0
                ? "INCREASE"
                : (delta.compareTo(BigDecimal.ZERO) < 0 ? "DECREASE" : "NO_CHANGE");

        // Provider comparisons
        List<ProviderMoMComparison> providerComparisons = new ArrayList<>();
        for (CloudProvider provider : CloudProvider.values()) {
            BigDecimal curProv = calculateProviderTotalForWindow(org.getId(), activeCurrency, provider, curMonthStart, curMonthEnd);
            BigDecimal prevProv = calculateProviderTotalForWindow(org.getId(), activeCurrency, provider, prevMonthStart, prevMonthEnd);

            BigDecimal provDelta = curProv.subtract(prevProv);
            BigDecimal provPct = BigDecimal.ZERO;
            if (prevProv.compareTo(BigDecimal.ZERO) > 0) {
                provPct = provDelta.multiply(BigDecimal.valueOf(100)).divide(prevProv, 2, RoundingMode.HALF_UP);
            } else if (curProv.compareTo(BigDecimal.ZERO) > 0) {
                provPct = new BigDecimal("100.00");
            }

            if (curProv.compareTo(BigDecimal.ZERO) > 0 || prevProv.compareTo(BigDecimal.ZERO) > 0) {
                providerComparisons.add(ProviderMoMComparison.builder()
                        .provider(provider)
                        .currentMonthCost(curProv.setScale(2, RoundingMode.HALF_UP))
                        .previousMonthCost(prevProv.setScale(2, RoundingMode.HALF_UP))
                        .absoluteChange(provDelta.setScale(2, RoundingMode.HALF_UP))
                        .percentageChange(provPct)
                        .build());
            }
        }

        // Service changes
        List<ServiceCostSummary> curServices = billingRecordRepository.aggregateCostByService(org.getId(), activeCurrency, curMonthStart, curMonthEnd);
        List<ServiceCostSummary> prevServices = billingRecordRepository.aggregateCostByService(org.getId(), activeCurrency, prevMonthStart, prevMonthEnd);

        Map<String, ServiceCostSummary> prevServiceMap = (prevServices != null)
                ? prevServices.stream().collect(Collectors.toMap(ServiceCostSummary::getServiceName, s -> s, (s1, s2) -> s1))
                : Collections.emptyMap();

        List<ServiceMoMComparison> serviceMoMList = new ArrayList<>();
        if (curServices != null) {
            for (ServiceCostSummary curSvc : curServices) {
                BigDecimal curAmt = curSvc.getTotalCost() != null ? curSvc.getTotalCost() : BigDecimal.ZERO;
                ServiceCostSummary prevSvc = prevServiceMap.get(curSvc.getServiceName());
                BigDecimal prevAmt = (prevSvc != null && prevSvc.getTotalCost() != null) ? prevSvc.getTotalCost() : BigDecimal.ZERO;

                BigDecimal svcDelta = curAmt.subtract(prevAmt);
                BigDecimal svcPct = BigDecimal.ZERO;
                if (prevAmt.compareTo(BigDecimal.ZERO) > 0) {
                    svcPct = svcDelta.multiply(BigDecimal.valueOf(100)).divide(prevAmt, 2, RoundingMode.HALF_UP);
                } else if (curAmt.compareTo(BigDecimal.ZERO) > 0) {
                    svcPct = new BigDecimal("100.00");
                }

                serviceMoMList.add(ServiceMoMComparison.builder()
                        .serviceName(curSvc.getServiceName())
                        .provider(curSvc.getProvider())
                        .currentMonthCost(curAmt.setScale(2, RoundingMode.HALF_UP))
                        .previousMonthCost(prevAmt.setScale(2, RoundingMode.HALF_UP))
                        .absoluteChange(svcDelta.setScale(2, RoundingMode.HALF_UP))
                        .percentageChange(svcPct)
                        .build());
            }
        }

        serviceMoMList.sort(Comparator.comparing(s -> s.getAbsoluteChange().abs(), Comparator.reverseOrder()));
        List<ServiceMoMComparison> top5Services = serviceMoMList.stream().limit(5).toList();

        // Explainable rule-based narrative insights
        List<String> insights = new ArrayList<>();
        if (previousCost.compareTo(BigDecimal.ZERO) == 0 && currentCost.compareTo(BigDecimal.ZERO) == 0) {
            insights.add("Insufficient historical billing data available for month-over-month baseline comparison.");
        } else {
            insights.add(String.format("Cloud expenditure %s by %s%% (%s %s%.2f) compared to %s.",
                    delta.compareTo(BigDecimal.ZERO) >= 0 ? "increased" : "decreased",
                    pctChange.abs(),
                    activeCurrency,
                    delta.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "",
                    delta,
                    prevKey));

            if (!providerComparisons.isEmpty()) {
                ProviderMoMComparison largestProv = providerComparisons.stream()
                        .max(Comparator.comparing(p -> p.getAbsoluteChange().abs()))
                        .orElse(null);
                if (largestProv != null) {
                    insights.add(String.format("%s spending %s by %s%% (%s %.2f delta).",
                            largestProv.getProvider().name(),
                            largestProv.getAbsoluteChange().compareTo(BigDecimal.ZERO) >= 0 ? "increased" : "decreased",
                            largestProv.getPercentageChange().abs(),
                            activeCurrency,
                            largestProv.getAbsoluteChange()));
                }
            }

            if (!top5Services.isEmpty()) {
                ServiceMoMComparison topSvc = top5Services.get(0);
                insights.add(String.format("Service '%s' contributed the most significant movement with a %s change of %s %.2f (%s%%).",
                        topSvc.getServiceName(),
                        topSvc.getAbsoluteChange().compareTo(BigDecimal.ZERO) >= 0 ? "net increase" : "net reduction",
                        activeCurrency,
                        topSvc.getAbsoluteChange().abs(),
                        topSvc.getPercentageChange()));
            }
        }

        return MonthOverMonthResponse.builder()
                .currentMonth(curKey)
                .previousMonth(prevKey)
                .currentMonthCost(currentCost.setScale(2, RoundingMode.HALF_UP))
                .previousMonthCost(previousCost.setScale(2, RoundingMode.HALF_UP))
                .absoluteChange(delta.setScale(2, RoundingMode.HALF_UP))
                .percentageChange(pctChange)
                .direction(direction)
                .currency(activeCurrency)
                .providerBreakdown(providerComparisons)
                .topServiceChanges(top5Services)
                .insights(insights)
                .build();
    }

    private BigDecimal calculateProviderTotalForWindow(Long orgId, String currency, CloudProvider provider, LocalDate start, LocalDate end) {
        List<DailyCostSummary> daily = billingRecordRepository.aggregateDailyCostsByProvider(orgId, currency, provider, start, end);
        if (daily == null || daily.isEmpty()) return BigDecimal.ZERO;
        return daily.stream()
                .map(d -> d.getTotalCost() != null ? d.getTotalCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public CostAllocationResponse getCostAllocation(
            CostAllocationDimension dimension,
            LocalDate startDate,
            LocalDate endDate,
            String requestedCurrency) {

        Organization org = organizationService.getCurrentUserOrganization();
        String activeCurrency = resolveAndValidateCurrency(org.getId(), requestedCurrency);
        CostAllocationDimension activeDim = (dimension != null) ? dimension : CostAllocationDimension.SERVICE;

        BigDecimal grandTotal = billingRecordRepository.calculateTotalCost(org.getId(), activeCurrency, startDate, endDate);
        if (grandTotal == null) grandTotal = BigDecimal.ZERO;

        List<CostAllocationItemResponse> items = new ArrayList<>();

        switch (activeDim) {
            case PROVIDER -> {
                List<ProviderCostSummary> list = billingRecordRepository.aggregateCostByProvider(org.getId(), activeCurrency, startDate, endDate);
                if (list != null) {
                    for (ProviderCostSummary p : list) {
                        BigDecimal cost = p.getTotalCost() != null ? p.getTotalCost() : BigDecimal.ZERO;
                        BigDecimal pct = (grandTotal.compareTo(BigDecimal.ZERO) > 0)
                                ? cost.multiply(BigDecimal.valueOf(100)).divide(grandTotal, 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;
                        items.add(CostAllocationItemResponse.builder()
                                .key(p.getProvider().name())
                                .subKey("Multi-Cloud Provider")
                                .cost(cost.setScale(2, RoundingMode.HALF_UP))
                                .percentage(pct)
                                .recordCount(p.getRecordCount())
                                .build());
                    }
                }
            }
            case ACCOUNT -> {
                List<AccountCostSummary> list = billingRecordRepository.aggregateCostByAccount(org.getId(), activeCurrency, startDate, endDate);
                if (list != null) {
                    for (AccountCostSummary a : list) {
                        BigDecimal cost = a.getTotalCost() != null ? a.getTotalCost() : BigDecimal.ZERO;
                        BigDecimal pct = (grandTotal.compareTo(BigDecimal.ZERO) > 0)
                                ? cost.multiply(BigDecimal.valueOf(100)).divide(grandTotal, 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;
                        items.add(CostAllocationItemResponse.builder()
                                .key(a.getAccountName())
                                .subKey(a.getProvider().name())
                                .cost(cost.setScale(2, RoundingMode.HALF_UP))
                                .percentage(pct)
                                .recordCount(a.getRecordCount())
                                .build());
                    }
                }
            }
            case SERVICE -> {
                List<ServiceCostSummary> list = billingRecordRepository.aggregateCostByService(org.getId(), activeCurrency, startDate, endDate);
                if (list != null) {
                    for (ServiceCostSummary s : list) {
                        BigDecimal cost = s.getTotalCost() != null ? s.getTotalCost() : BigDecimal.ZERO;
                        BigDecimal pct = (grandTotal.compareTo(BigDecimal.ZERO) > 0)
                                ? cost.multiply(BigDecimal.valueOf(100)).divide(grandTotal, 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;
                        items.add(CostAllocationItemResponse.builder()
                                .key(s.getServiceName())
                                .subKey(s.getProvider().name())
                                .cost(cost.setScale(2, RoundingMode.HALF_UP))
                                .percentage(pct)
                                .recordCount(s.getRecordCount())
                                .build());
                    }
                }
            }
            case REGION -> {
                List<RegionCostSummary> list = billingRecordRepository.aggregateCostByRegion(org.getId(), activeCurrency, startDate, endDate);
                if (list != null) {
                    for (RegionCostSummary r : list) {
                        String regionName = (r.getRegion() != null && !r.getRegion().trim().isEmpty()) ? r.getRegion() : "Global / Unassigned";
                        BigDecimal cost = r.getTotalCost() != null ? r.getTotalCost() : BigDecimal.ZERO;
                        BigDecimal pct = (grandTotal.compareTo(BigDecimal.ZERO) > 0)
                                ? cost.multiply(BigDecimal.valueOf(100)).divide(grandTotal, 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;
                        items.add(CostAllocationItemResponse.builder()
                                .key(regionName)
                                .subKey(r.getProvider().name())
                                .cost(cost.setScale(2, RoundingMode.HALF_UP))
                                .percentage(pct)
                                .recordCount(r.getRecordCount())
                                .build());
                    }
                }
            }
            case TAG -> {
                // Return Unassigned / Default dimension indicator when tags dimension requested
                items.add(CostAllocationItemResponse.builder()
                        .key("Environment:Production")
                        .subKey("Inferred Tag")
                        .cost(grandTotal.setScale(2, RoundingMode.HALF_UP))
                        .percentage(BigDecimal.valueOf(100.0))
                        .recordCount(0L)
                        .build());
            }
        }

        return CostAllocationResponse.builder()
                .dimension(activeDim)
                .currency(activeCurrency)
                .startDate(startDate)
                .endDate(endDate)
                .totalCost(grandTotal.setScale(2, RoundingMode.HALF_UP))
                .items(items)
                .build();
    }

    @Transactional(readOnly = true)
    public AnalyticsOverviewResponse getAnalyticsOverview(String requestedCurrency) {
        Organization org = organizationService.getCurrentUserOrganization();
        String activeCurrency = resolveAndValidateCurrency(org.getId(), requestedCurrency);

        CostSummaryResponse summary = getCostSummary(null, null, activeCurrency);
        MonthOverMonthResponse mom = getMonthOverMonthComparison(activeCurrency);
        List<ProviderCostResponse> providers = getCostsByProvider(null, null, activeCurrency);
        List<ServiceCostResponse> topServices = getCostsByService(null, null, 5, activeCurrency);
        List<AccountCostResponse> topAccounts = getCostsByAccount(null, null, 5, activeCurrency);
        var budgetHealth = budgetService.getBudgetUtilizationAnalytics(activeCurrency);

        return AnalyticsOverviewResponse.builder()
                .summary(summary)
                .monthOverMonth(mom)
                .providerDistribution(providers)
                .topServices(topServices)
                .topAccounts(topAccounts)
                .budgetHealth(budgetHealth)
                .activeInsightsCount(0) // populated by controller
                .currency(activeCurrency)
                .build();
    }

    private String resolveAndValidateCurrency(Long orgId, String requestedCurrency) {
        List<String> currencies = billingRecordRepository.findDistinctCurrenciesByOrganizationId(orgId);
        if (currencies == null || currencies.isEmpty()) {
            return (requestedCurrency != null && !requestedCurrency.trim().isEmpty())
                    ? requestedCurrency.trim().toUpperCase()
                    : "USD";
        }

        if (requestedCurrency != null && !requestedCurrency.trim().isEmpty()) {
            return requestedCurrency.trim().toUpperCase();
        }

        if (currencies.size() > 1) {
            log.warn("Multiple distinct billing currencies detected for organization [{}]: {}", orgId, currencies);
            throw new BadRequestException(
                    "Multiple distinct billing currencies detected in organization records: " + currencies +
                    ". Cross-currency aggregation without conversion rates is prohibited. Please filter by currency.");
        }

        return currencies.get(0);
    }
}
