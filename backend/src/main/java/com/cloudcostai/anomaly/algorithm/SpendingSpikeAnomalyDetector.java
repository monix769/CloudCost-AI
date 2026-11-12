package com.cloudcostai.anomaly.algorithm;

import com.cloudcostai.entity.AnomalyDetectionConfig;
import com.cloudcostai.entity.AnomalyDetectionMethod;
import com.cloudcostai.entity.AnomalySeverity;
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
public class SpendingSpikeAnomalyDetector implements AnomalyDetector {

    @Override
    public AnomalyDetectionMethod getMethod() {
        return AnomalyDetectionMethod.SPENDING_SPIKE;
    }

    @Override
    public List<AnomalyCandidate> detect(
            CloudAccount account,
            String serviceName,
            String region,
            Map<LocalDate, BigDecimal> dailyCosts,
            LocalDate targetDate,
            AnomalyDetectionConfig config) {

        BigDecimal targetSpend = dailyCosts.get(targetDate);
        if (targetSpend == null || targetSpend.compareTo(BigDecimal.ZERO) <= 0) {
            return Collections.emptyList();
        }

        BigDecimal spikeThresholdPct = config != null && config.getSpendingSpikePercent() != null
                ? config.getSpendingSpikePercent()
                : new BigDecimal("50.00");

        BigDecimal minExcessAmount = config != null && config.getSpendingSpikeMinAmount() != null
                ? config.getSpendingSpikeMinAmount()
                : new BigDecimal("50.0000");

        // Find immediately preceding available date
        LocalDate previousDate = dailyCosts.keySet().stream()
                .filter(d -> d.isBefore(targetDate))
                .max(LocalDate::compareTo)
                .orElse(null);

        if (previousDate == null) {
            return Collections.emptyList();
        }

        BigDecimal previousSpend = dailyCosts.get(previousDate);
        if (previousSpend == null || previousSpend.compareTo(BigDecimal.ZERO) <= 0) {
            // Previous day had zero registered cost; if today spent significantly above minimum threshold:
            if (targetSpend.compareTo(minExcessAmount) >= 0) {
                String dimensionKey = "account:" + account.getId() + ":service:" + serviceName.toLowerCase().trim() + ":date:" + targetDate;
                String rootCause = String.format(
                        "Sudden spending spike detected for %s. Spend escalated from $0.00 on %s to $%s on %s.",
                        serviceName, previousDate, targetSpend.setScale(2, RoundingMode.HALF_UP), targetDate);
                String recommendation = String.format(
                        "Investigate newly provisioned %s cloud resources or sudden cluster activation on %s.",
                        serviceName, targetDate);

                AnomalyCandidate candidate = AnomalyCandidate.builder()
                        .cloudAccount(account)
                        .serviceName(serviceName)
                        .region(region)
                        .expectedCost(BigDecimal.ZERO)
                        .actualCost(targetSpend)
                        .excessCost(targetSpend)
                        .deviationPercentage(new BigDecimal("100.00"))
                        .severity(targetSpend.compareTo(new BigDecimal("500.00")) >= 0 ? AnomalySeverity.CRITICAL : AnomalySeverity.HIGH)
                        .detectionMethod(AnomalyDetectionMethod.SPENDING_SPIKE)
                        .anomalyPeriodStart(targetDate)
                        .anomalyPeriodEnd(targetDate)
                        .dimensionKey(dimensionKey)
                        .rootCauseSummary(rootCause)
                        .recommendedAction(recommendation)
                        .build();

                List<AnomalyCandidate> result = new ArrayList<>(1);
                result.add(candidate);
                return result;
            }
            return Collections.emptyList();
        }

        BigDecimal excessCost = targetSpend.subtract(previousSpend).setScale(4, RoundingMode.HALF_UP);
        if (excessCost.compareTo(minExcessAmount) < 0) {
            return Collections.emptyList();
        }

        BigDecimal spikePct = excessCost.divide(previousSpend, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);

        if (spikePct.compareTo(spikeThresholdPct) >= 0) {
            AnomalySeverity severity;
            if (spikePct.compareTo(new BigDecimal("200.00")) >= 0 || excessCost.compareTo(new BigDecimal("500.00")) >= 0) {
                severity = AnomalySeverity.CRITICAL;
            } else if (spikePct.compareTo(new BigDecimal("100.00")) >= 0 || excessCost.compareTo(new BigDecimal("150.00")) >= 0) {
                severity = AnomalySeverity.HIGH;
            } else if (spikePct.compareTo(new BigDecimal("50.00")) >= 0) {
                severity = AnomalySeverity.MEDIUM;
            } else {
                severity = AnomalySeverity.LOW;
            }

            String dimensionKey = "account:" + account.getId() + ":service:" + serviceName.toLowerCase().trim() + ":date:" + targetDate;

            String rootCause = String.format(
                    "Sudden daily spending spike detected for %s. Daily cost surged by +%s%% from $%s (%s) to $%s (%s).",
                    serviceName, spikePct, previousSpend.setScale(2, RoundingMode.HALF_UP), previousDate,
                    targetSpend.setScale(2, RoundingMode.HALF_UP), targetDate);

            String recommendation = String.format(
                    "Check for runaway batch processes, unthrottled worker queues, or misconfigured retry loops " +
                    "in %s occurring on %s.",
                    serviceName, targetDate);

            AnomalyCandidate candidate = AnomalyCandidate.builder()
                    .cloudAccount(account)
                    .serviceName(serviceName)
                    .region(region)
                    .expectedCost(previousSpend)
                    .actualCost(targetSpend)
                    .excessCost(excessCost)
                    .deviationPercentage(spikePct)
                    .severity(severity)
                    .detectionMethod(AnomalyDetectionMethod.SPENDING_SPIKE)
                    .anomalyPeriodStart(targetDate)
                    .anomalyPeriodEnd(targetDate)
                    .dimensionKey(dimensionKey)
                    .rootCauseSummary(rootCause)
                    .recommendedAction(recommendation)
                    .build();

            List<AnomalyCandidate> result = new ArrayList<>(1);
            result.add(candidate);
            return result;
        }

        return Collections.emptyList();
    }
}

