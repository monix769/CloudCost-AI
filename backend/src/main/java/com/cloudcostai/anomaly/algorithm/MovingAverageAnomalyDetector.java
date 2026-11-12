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
public class MovingAverageAnomalyDetector implements AnomalyDetector {

    private static final int MIN_HISTORICAL_DAYS = 3;
    private static final BigDecimal MIN_EXCESS_COST = new BigDecimal("20.00");

    @Override
    public AnomalyDetectionMethod getMethod() {
        return AnomalyDetectionMethod.MOVING_AVERAGE;
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

        int windowDays = config != null && config.getMovingAvgWindowDays() != null
                ? config.getMovingAvgWindowDays()
                : 14;

        BigDecimal thresholdPct = config != null && config.getMovingAvgDeviationPercent() != null
                ? config.getMovingAvgDeviationPercent()
                : new BigDecimal("30.00");

        // Filter and collect the latest k historical days strictly before targetDate
        List<BigDecimal> windowValues = dailyCosts.entrySet().stream()
                .filter(e -> e.getKey().isBefore(targetDate))
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .toList();

        if (windowValues.size() < MIN_HISTORICAL_DAYS) {
            return Collections.emptyList();
        }

        // Take only the last 'windowDays' elements
        int startIndex = Math.max(0, windowValues.size() - windowDays);
        List<BigDecimal> recentWindow = windowValues.subList(startIndex, windowValues.size());

        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal val : recentWindow) {
            sum = sum.add(val);
        }
        BigDecimal movingAvg = sum.divide(BigDecimal.valueOf(recentWindow.size()), 4, RoundingMode.HALF_UP);

        if (movingAvg.compareTo(BigDecimal.ZERO) <= 0) {
            return Collections.emptyList();
        }

        BigDecimal excessCost = targetSpend.subtract(movingAvg).setScale(4, RoundingMode.HALF_UP);
        if (excessCost.compareTo(MIN_EXCESS_COST) < 0) {
            return Collections.emptyList();
        }

        BigDecimal deviationPct = excessCost.divide(movingAvg, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);

        if (deviationPct.compareTo(thresholdPct) >= 0) {
            AnomalySeverity severity;
            if (deviationPct.compareTo(new BigDecimal("150.00")) >= 0) {
                severity = AnomalySeverity.CRITICAL;
            } else if (deviationPct.compareTo(new BigDecimal("75.00")) >= 0) {
                severity = AnomalySeverity.HIGH;
            } else if (deviationPct.compareTo(new BigDecimal("40.00")) >= 0) {
                severity = AnomalySeverity.MEDIUM;
            } else {
                severity = AnomalySeverity.LOW;
            }

            String dimensionKey = "account:" + account.getId() + ":service:" + serviceName.toLowerCase().trim() + ":date:" + targetDate;

            String rootCause = String.format(
                    "Moving average deviation anomaly detected for %s. Daily cost of $%s represents a +%s%% surge " +
                    "over the %d-day moving average baseline of $%s.",
                    serviceName, targetSpend.setScale(2, RoundingMode.HALF_UP), deviationPct,
                    recentWindow.size(), movingAvg.setScale(2, RoundingMode.HALF_UP));

            String recommendation = String.format(
                    "Examine continuous workload changes for %s over the past %d days. Verify whether a recent deployment " +
                    "or increased API ingestion throughput introduced elevated steady-state costs.",
                    serviceName, recentWindow.size());

            AnomalyCandidate candidate = AnomalyCandidate.builder()
                    .cloudAccount(account)
                    .serviceName(serviceName)
                    .region(region)
                    .expectedCost(movingAvg)
                    .actualCost(targetSpend)
                    .excessCost(excessCost)
                    .deviationPercentage(deviationPct)
                    .severity(severity)
                    .detectionMethod(AnomalyDetectionMethod.MOVING_AVERAGE)
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

