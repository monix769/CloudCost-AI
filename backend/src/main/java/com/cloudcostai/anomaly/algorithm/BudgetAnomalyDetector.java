package com.cloudcostai.anomaly.algorithm;

import com.cloudcostai.entity.AnomalyDetectionConfig;
import com.cloudcostai.entity.AnomalyDetectionMethod;
import com.cloudcostai.entity.AnomalySeverity;
import com.cloudcostai.entity.Budget;
import com.cloudcostai.entity.CloudAccount;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class BudgetAnomalyDetector implements AnomalyDetector {

    @Override
    public AnomalyDetectionMethod getMethod() {
        return AnomalyDetectionMethod.BUDGET_BASED;
    }

    @Override
    public List<AnomalyCandidate> detect(
            CloudAccount account,
            String serviceName,
            String region,
            Map<LocalDate, BigDecimal> dailyCosts,
            LocalDate targetDate,
            AnomalyDetectionConfig config) {
        // Daily time-series detector contract; budgets are evaluated via detectBudgetAnomaly
        return Collections.emptyList();
    }

    /**
     * Evaluates a budget entity against configured utilization thresholds.
     */
    public List<AnomalyCandidate> detectBudgetAnomaly(
            Budget budget,
            CloudAccount associatedAccount,
            AnomalyDetectionConfig config) {

        if (budget == null || budget.getAmountLimit() == null || budget.getAmountLimit().compareTo(BigDecimal.ZERO) <= 0) {
            return Collections.emptyList();
        }

        BigDecimal limit = budget.getAmountLimit();
        BigDecimal currentSpend = budget.getCurrentSpend() != null ? budget.getCurrentSpend() : BigDecimal.ZERO;

        BigDecimal utilizationPct = currentSpend.divide(limit, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);

        BigDecimal criticalThreshold = config != null && config.getBudgetCriticalThresholdPercent() != null
                ? config.getBudgetCriticalThresholdPercent()
                : new BigDecimal("100.00");

        BigDecimal warningThreshold = config != null && config.getBudgetWarningThresholdPercent() != null
                ? config.getBudgetWarningThresholdPercent()
                : new BigDecimal("90.00");

        if (utilizationPct.compareTo(warningThreshold) < 0) {
            return Collections.emptyList();
        }

        AnomalySeverity severity;
        BigDecimal excessCost;
        String rootCause;
        String recommendation;

        if (utilizationPct.compareTo(criticalThreshold) >= 0) {
            severity = AnomalySeverity.CRITICAL;
            excessCost = currentSpend.subtract(limit).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
            rootCause = String.format(
                    "Budget limit breach: '%s' has exceeded its allocated limit (Spend: $%s, Limit: $%s, Utilization: %s%%).",
                    budget.getName(), currentSpend.setScale(2, RoundingMode.HALF_UP),
                    limit.setScale(2, RoundingMode.HALF_UP), utilizationPct);
            recommendation = String.format(
                    "Immediate executive FinOps action required for '%s'. Reallocate budget funds or freeze non-essential workloads.",
                    budget.getName());
        } else {
            severity = AnomalySeverity.HIGH;
            excessCost = currentSpend.subtract(limit.multiply(new BigDecimal("0.90"))).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
            rootCause = String.format(
                    "Budget threshold warning: '%s' has reached %s%% utilization ($%s of $%s allocation).",
                    budget.getName(), utilizationPct, currentSpend.setScale(2, RoundingMode.HALF_UP),
                    limit.setScale(2, RoundingMode.HALF_UP));
            recommendation = String.format(
                    "Monitor burn rate for '%s'. Audit test/dev resources and pause non-critical batch compute tasks.",
                    budget.getName());
        }

        String dimensionKey = "budget:" + budget.getId() + ":period:" + budget.getPeriod();
        LocalDate now = LocalDate.now();

        AnomalyCandidate candidate = AnomalyCandidate.builder()
                .cloudAccount(associatedAccount)
                .serviceName("Budget: " + budget.getName())
                .region("GLOBAL")
                .expectedCost(limit)
                .actualCost(currentSpend)
                .excessCost(excessCost)
                .deviationPercentage(utilizationPct.subtract(new BigDecimal("100.00")).max(BigDecimal.ZERO))
                .severity(severity)
                .detectionMethod(AnomalyDetectionMethod.BUDGET_BASED)
                .anomalyPeriodStart(now.withDayOfMonth(1))
                .anomalyPeriodEnd(now)
                .dimensionKey(dimensionKey)
                .rootCauseSummary(rootCause)
                .recommendedAction(recommendation)
                .build();

        List<AnomalyCandidate> result = new ArrayList<>(1);
        result.add(candidate);
        return result;
    }
}
