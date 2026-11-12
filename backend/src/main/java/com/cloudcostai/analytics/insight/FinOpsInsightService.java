package com.cloudcostai.analytics.insight;

import com.cloudcostai.dto.BudgetResponse;
import com.cloudcostai.entity.CloudAccount;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.repository.BillingRecordRepository;
import com.cloudcostai.repository.CloudAccountRepository;
import com.cloudcostai.repository.projection.AccountCostSummary;
import com.cloudcostai.repository.projection.ProviderCostSummary;
import com.cloudcostai.repository.projection.ServiceCostSummary;
import com.cloudcostai.service.BudgetService;
import com.cloudcostai.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinOpsInsightService {

    private final BillingRecordRepository billingRecordRepository;
    private final CloudAccountRepository cloudAccountRepository;
    private final BudgetService budgetService;
    private final OrganizationService organizationService;

    @Transactional(readOnly = true)
    public List<FinOpsInsight> generateInsights(String requestedCurrency) {
        Organization org = organizationService.getCurrentUserOrganization();
        Long orgId = org.getId();

        String activeCurrency = resolveCurrency(orgId, requestedCurrency);
        List<FinOpsInsight> insights = new ArrayList<>();

        // 1. Month-over-Month Trend Rule
        evaluateMonthOverMonthSpike(orgId, activeCurrency, insights);

        // 2. Service Dominance & Optimization Rule
        evaluateServiceConcentration(orgId, activeCurrency, insights);

        // 3. Provider Concentration Rule
        evaluateProviderConcentration(orgId, activeCurrency, insights);

        // 4. Cloud Account Concentration Rule
        evaluateAccountConcentration(orgId, activeCurrency, insights);

        // 5. Budget Utilization & Exhaustion Rules
        evaluateBudgetRisks(activeCurrency, insights);

        // 6. Data Governance & Sync Freshness Rule
        evaluateSyncFreshness(orgId, insights);

        // Sort descending by severity: CRITICAL > HIGH > MEDIUM > LOW
        insights.sort(Comparator.comparing(FinOpsInsight::getSeverity, Comparator.comparingInt(this::severityWeight).reversed()));

        log.debug("Evaluated FinOps insight engine for Org [{}]: generated {} actionable insights", orgId, insights.size());
        return insights;
    }

    private int severityWeight(InsightSeverity severity) {
        return switch (severity) {
            case CRITICAL -> 4;
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }

    private void evaluateMonthOverMonthSpike(Long orgId, String currency, List<FinOpsInsight> insights) {
        LocalDate now = LocalDate.now();
        LocalDate currentMonthStart = now.withDayOfMonth(1);
        LocalDate currentMonthEnd = now.with(TemporalAdjusters.lastDayOfMonth());

        LocalDate prevMonthDate = now.minusMonths(1);
        LocalDate prevMonthStart = prevMonthDate.withDayOfMonth(1);
        LocalDate prevMonthEnd = prevMonthDate.with(TemporalAdjusters.lastDayOfMonth());

        BigDecimal currentCost = billingRecordRepository.calculateTotalCost(orgId, currency, currentMonthStart, currentMonthEnd);
        BigDecimal prevCost = billingRecordRepository.calculateTotalCost(orgId, currency, prevMonthStart, prevMonthEnd);

        if (currentCost == null) currentCost = BigDecimal.ZERO;
        if (prevCost == null) prevCost = BigDecimal.ZERO;

        if (prevCost.compareTo(BigDecimal.ZERO) > 0 && currentCost.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal delta = currentCost.subtract(prevCost);
            BigDecimal percentage = delta.multiply(BigDecimal.valueOf(100)).divide(prevCost, 2, RoundingMode.HALF_UP);

            if (percentage.compareTo(BigDecimal.valueOf(15.0)) >= 0) {
                InsightSeverity severity = percentage.compareTo(BigDecimal.valueOf(30.0)) >= 0
                        ? InsightSeverity.CRITICAL
                        : InsightSeverity.HIGH;

                insights.add(FinOpsInsight.builder()
                        .id("INS-MOM-INCREASE-" + currentMonthStart.getYear() + "-" + currentMonthStart.getMonthValue())
                        .title("Significant Month-Over-Month Cost Acceleration")
                        .description(String.format("Current month expenditure (%s %.2f) has grown by %s%% compared to the previous month (%s %.2f).",
                                currency, currentCost, percentage, currency, prevCost))
                        .severity(severity)
                        .financialImpact(delta.setScale(2, RoundingMode.HALF_UP))
                        .currency(currency)
                        .category("COST_SPIKE")
                        .recommendation("Audit recent infrastructure deployments and provisioned autoscaling policies to prevent uncontrolled budget overrun.")
                        .detectionTimestamp(Instant.now())
                        .build());
            } else if (percentage.compareTo(BigDecimal.valueOf(-15.0)) <= 0) {
                insights.add(FinOpsInsight.builder()
                        .id("INS-MOM-REDUCTION-" + currentMonthStart.getYear() + "-" + currentMonthStart.getMonthValue())
                        .title("Sustained Cloud Cost Reduction")
                        .description(String.format("Current month spend is %s%% lower than the previous month, reflecting successful resource decommissioning or right-sizing.",
                                percentage.abs()))
                        .severity(InsightSeverity.LOW)
                        .financialImpact(delta.abs().setScale(2, RoundingMode.HALF_UP))
                        .currency(currency)
                        .category("OPTIMIZATION")
                        .recommendation("Document current cost governance policies as organizational best practices.")
                        .detectionTimestamp(Instant.now())
                        .build());
            }
        }
    }

    private void evaluateServiceConcentration(Long orgId, String currency, List<FinOpsInsight> insights) {
        BigDecimal totalSpend = billingRecordRepository.calculateTotalCost(orgId, currency, null, null);
        if (totalSpend == null || totalSpend.compareTo(BigDecimal.ZERO) == 0) return;

        List<ServiceCostSummary> serviceSummaries = billingRecordRepository.aggregateCostByService(orgId, currency, null, null);
        if (serviceSummaries != null && !serviceSummaries.isEmpty()) {
            ServiceCostSummary top = serviceSummaries.get(0);
            BigDecimal topCost = top.getTotalCost() != null ? top.getTotalCost() : BigDecimal.ZERO;
            BigDecimal share = topCost.multiply(BigDecimal.valueOf(100)).divide(totalSpend, 2, RoundingMode.HALF_UP);

            if (share.compareTo(BigDecimal.valueOf(40.0)) >= 0) {
                insights.add(FinOpsInsight.builder()
                        .id("INS-SERVICE-CONC-" + top.getServiceName().replaceAll("\\s+", "-"))
                        .title("High Expenditure Concentration on " + top.getServiceName())
                        .description(String.format("Service '%s' represents %s%% (%s %.2f) of total organization cloud spend.",
                                top.getServiceName(), share, currency, topCost))
                        .severity(share.compareTo(BigDecimal.valueOf(60.0)) >= 0 ? InsightSeverity.HIGH : InsightSeverity.MEDIUM)
                        .relatedProvider(top.getProvider())
                        .relatedService(top.getServiceName())
                        .financialImpact(topCost.setScale(2, RoundingMode.HALF_UP))
                        .currency(currency)
                        .category("CONCENTRATION")
                        .recommendation("Investigate 1-year or 3-year Reserved Instances (RI), Savings Plans, or Committed Use Discounts to reduce baseline cost by 30-50%.")
                        .detectionTimestamp(Instant.now())
                        .build());
            }
        }
    }

    private void evaluateProviderConcentration(Long orgId, String currency, List<FinOpsInsight> insights) {
        BigDecimal totalSpend = billingRecordRepository.calculateTotalCost(orgId, currency, null, null);
        if (totalSpend == null || totalSpend.compareTo(BigDecimal.ZERO) == 0) return;

        List<ProviderCostSummary> providerSummaries = billingRecordRepository.aggregateCostByProvider(orgId, currency, null, null);
        if (providerSummaries != null && providerSummaries.size() > 1) {
            ProviderCostSummary dominant = providerSummaries.get(0);
            BigDecimal providerCost = dominant.getTotalCost() != null ? dominant.getTotalCost() : BigDecimal.ZERO;
            BigDecimal share = providerCost.multiply(BigDecimal.valueOf(100)).divide(totalSpend, 2, RoundingMode.HALF_UP);

            if (share.compareTo(BigDecimal.valueOf(80.0)) >= 0) {
                insights.add(FinOpsInsight.builder()
                        .id("INS-PROVIDER-CONC-" + dominant.getProvider().name())
                        .title("Single Provider Spend Concentration (" + dominant.getProvider().name() + ")")
                        .description(String.format("Cloud provider %s accounts for %s%% (%s %.2f) of overall multi-cloud expenditure.",
                                dominant.getProvider().name(), share, currency, providerCost))
                        .severity(InsightSeverity.LOW)
                        .relatedProvider(dominant.getProvider())
                        .financialImpact(providerCost.setScale(2, RoundingMode.HALF_UP))
                        .currency(currency)
                        .category("CONCENTRATION")
                        .recommendation("Leverage high volume tiering to negotiate enterprise discount agreements (EDP) with " + dominant.getProvider().name() + ".")
                        .detectionTimestamp(Instant.now())
                        .build());
            }
        }
    }

    private void evaluateAccountConcentration(Long orgId, String currency, List<FinOpsInsight> insights) {
        BigDecimal totalSpend = billingRecordRepository.calculateTotalCost(orgId, currency, null, null);
        if (totalSpend == null || totalSpend.compareTo(BigDecimal.ZERO) == 0) return;

        List<AccountCostSummary> accounts = billingRecordRepository.aggregateCostByAccount(orgId, currency, null, null);
        if (accounts != null && accounts.size() > 1) {
            AccountCostSummary dominant = accounts.get(0);
            BigDecimal cost = dominant.getTotalCost() != null ? dominant.getTotalCost() : BigDecimal.ZERO;
            BigDecimal share = cost.multiply(BigDecimal.valueOf(100)).divide(totalSpend, 2, RoundingMode.HALF_UP);

            if (share.compareTo(BigDecimal.valueOf(65.0)) >= 0) {
                insights.add(FinOpsInsight.builder()
                        .id("INS-ACCOUNT-DOMINANCE-" + dominant.getAccountId())
                        .title("Cloud Account Dominance: " + dominant.getAccountName())
                        .description(String.format("Cloud account '%s' represents %s%% of total multi-cloud expenditure.",
                                dominant.getAccountName(), share))
                        .severity(InsightSeverity.MEDIUM)
                        .relatedProvider(dominant.getProvider())
                        .relatedAccount(dominant.getAccountName())
                        .financialImpact(cost.setScale(2, RoundingMode.HALF_UP))
                        .currency(currency)
                        .category("GOVERNANCE")
                        .recommendation("Review workload distribution to separate production, staging, and development workloads into dedicated accounts.")
                        .detectionTimestamp(Instant.now())
                        .build());
            }
        }
    }

    private void evaluateBudgetRisks(String currency, List<FinOpsInsight> insights) {
        List<BudgetResponse> budgets = budgetService.getAllBudgets();
        for (BudgetResponse budget : budgets) {
            BigDecimal limit = budget.getAmountLimit();
            BigDecimal spend = budget.getCurrentSpend() != null ? budget.getCurrentSpend() : BigDecimal.ZERO;

            if (limit != null && limit.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal utilization = spend.multiply(BigDecimal.valueOf(100)).divide(limit, 2, RoundingMode.HALF_UP);

                if (utilization.compareTo(BigDecimal.valueOf(100.0)) >= 0) {
                    BigDecimal overrun = spend.subtract(limit);
                    insights.add(FinOpsInsight.builder()
                            .id("INS-BUDGET-EXCEEDED-" + budget.getId())
                            .title("Budget Exceeded: " + budget.getName())
                            .description(String.format("Budget '%s' has reached %s%% utilization, exceeding limit by %s %.2f.",
                                    budget.getName(), utilization, currency, overrun))
                            .severity(InsightSeverity.CRITICAL)
                            .financialImpact(overrun.setScale(2, RoundingMode.HALF_UP))
                            .currency(currency)
                            .category("BUDGET_RISK")
                            .recommendation("Trigger automated notification to cost center owner and evaluate immediate shutdown of non-critical test environments.")
                            .detectionTimestamp(Instant.now())
                            .build());
                } else if (utilization.compareTo(BigDecimal.valueOf(80.0)) >= 0) {
                    insights.add(FinOpsInsight.builder()
                            .id("INS-BUDGET-WARNING-" + budget.getId())
                            .title("Budget Threshold Approaching: " + budget.getName())
                            .description(String.format("Budget '%s' is at %s%% capacity (%s %.2f of %s %.2f limit).",
                                    budget.getName(), utilization, currency, spend, currency, limit))
                            .severity(InsightSeverity.HIGH)
                            .financialImpact(spend.setScale(2, RoundingMode.HALF_UP))
                            .currency(currency)
                            .category("BUDGET_RISK")
                            .recommendation("Audit daily consumption velocity to avoid breaching limit before period end.")
                            .detectionTimestamp(Instant.now())
                            .build());
                }
            }
        }
    }

    private void evaluateSyncFreshness(Long orgId, List<FinOpsInsight> insights) {
        List<CloudAccount> accounts = cloudAccountRepository.findByOrganizationId(orgId);
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);

        for (CloudAccount account : accounts) {
            if (Boolean.TRUE.equals(account.getIsActive())) {
                boolean isStale = (account.getLastSyncedAt() == null || account.getLastSyncedAt().isBefore(sevenDaysAgo));
                boolean isFailed = "FAILED".equalsIgnoreCase(account.getSyncStatus());

                if (isFailed || isStale) {
                    String reason = isFailed ? "synchronization failed" : "has not synchronized billing data in over 7 days";
                    insights.add(FinOpsInsight.builder()
                            .id("INS-SYNC-STALE-" + account.getId())
                            .title("Stale Billing Synchronization: " + account.getAccountName())
                            .description(String.format("Cloud account '%s' (%s) %s. Analytics and forecasts may be incomplete.",
                                    account.getAccountName(), account.getProvider(), reason))
                            .severity(InsightSeverity.MEDIUM)
                            .relatedProvider(account.getProvider())
                            .relatedAccount(account.getAccountName())
                            .category("GOVERNANCE")
                            .recommendation("Verify provider credentials and trigger a manual synchronization in Cloud Accounts.")
                            .detectionTimestamp(Instant.now())
                            .build());
                }
            }
        }
    }

    private String resolveCurrency(Long orgId, String requestedCurrency) {
        List<String> currencies = billingRecordRepository.findDistinctCurrenciesByOrganizationId(orgId);
        if (currencies == null || currencies.isEmpty()) {
            return (requestedCurrency != null && !requestedCurrency.trim().isEmpty())
                    ? requestedCurrency.trim().toUpperCase()
                    : "USD";
        }
        if (requestedCurrency != null && !requestedCurrency.trim().isEmpty()) {
            return requestedCurrency.trim().toUpperCase();
        }
        return currencies.get(0);
    }
}

