package com.cloudcostai.anomaly.algorithm;

import com.cloudcostai.entity.AnomalyDetectionConfig;
import com.cloudcostai.entity.AnomalyDetectionMethod;
import com.cloudcostai.entity.AnomalySeverity;
import com.cloudcostai.entity.CloudAccount;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class ZScoreAnomalyDetector implements AnomalyDetector {

    private static final int MIN_HISTORICAL_DAYS = 4;
    private static final BigDecimal MIN_DOLLAR_SURGE = new BigDecimal("15.00");

    @Override
    public AnomalyDetectionMethod getMethod() {
        return AnomalyDetectionMethod.Z_SCORE;
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

        // Collect historical dates strictly before targetDate
        List<BigDecimal> historicalSpends = dailyCosts.entrySet().stream()
                .filter(entry -> entry.getKey().isBefore(targetDate))
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .toList();

        if (historicalSpends.size() < MIN_HISTORICAL_DAYS) {
            return Collections.emptyList();
        }

        // Calculate sample mean (mu)
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal val : historicalSpends) {
            sum = sum.add(val);
        }
        BigDecimal n = BigDecimal.valueOf(historicalSpends.size());
        BigDecimal mean = sum.divide(n, 4, RoundingMode.HALF_UP);

        // Calculate sample variance and standard deviation (sigma)
        BigDecimal sumSquaredDiff = BigDecimal.ZERO;
        for (BigDecimal val : historicalSpends) {
            BigDecimal diff = val.subtract(mean);
            sumSquaredDiff = sumSquaredDiff.add(diff.multiply(diff));
        }
        BigDecimal sampleVariance = sumSquaredDiff.divide(BigDecimal.valueOf(historicalSpends.size() - 1), 6, RoundingMode.HALF_UP);
        double stdDev = Math.sqrt(sampleVariance.doubleValue());

        double zScore;
        if (stdDev <= 0.0001) {
            // Safe handling of zero standard deviation (constant historical baseline)
            if (targetSpend.compareTo(mean) > 0 && mean.compareTo(BigDecimal.ZERO) > 0) {
                // If spend increased over a flat non-zero baseline, estimate Z relative to mean
                double relativeIncrease = targetSpend.subtract(mean).divide(mean, 4, RoundingMode.HALF_UP).doubleValue();
                zScore = relativeIncrease * 3.0; // scale factor
            } else {
                zScore = 0.0;
            }
        } else {
            zScore = (targetSpend.doubleValue() - mean.doubleValue()) / stdDev;
        }

        double threshold = config != null && config.getZScoreThreshold() != null
                ? config.getZScoreThreshold().doubleValue()
                : 2.50;

        BigDecimal excessCost = targetSpend.subtract(mean).setScale(4, RoundingMode.HALF_UP);

        if (zScore >= threshold && excessCost.compareTo(MIN_DOLLAR_SURGE) >= 0) {
            BigDecimal deviationPct = mean.compareTo(BigDecimal.ZERO) > 0
                    ? excessCost.divide(mean, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                    : new BigDecimal("100.00");

            AnomalySeverity severity;
            if (zScore >= 4.0 || deviationPct.compareTo(new BigDecimal("200.00")) >= 0) {
                severity = AnomalySeverity.CRITICAL;
            } else if (zScore >= 3.0 || deviationPct.compareTo(new BigDecimal("100.00")) >= 0) {
                severity = AnomalySeverity.HIGH;
            } else if (zScore >= 2.5) {
                severity = AnomalySeverity.MEDIUM;
            } else {
                severity = AnomalySeverity.LOW;
            }

            String dimensionKey = "account:" + account.getId() + ":service:" + serviceName.toLowerCase().trim() + ":date:" + targetDate;

            String rootCause = String.format(
                    "Z-Score statistical anomaly detected for %s (Z = %.2f, threshold = %.2f). " +
                    "Daily expenditure of $%s exceeded historical baseline mean of $%s (σ = $%.2f).",
                    serviceName, zScore, threshold, targetSpend.setScale(2, RoundingMode.HALF_UP),
                    mean.setScale(2, RoundingMode.HALF_UP), stdDev);

            String recommendation = String.format(
                    "Conduct FinOps drill-down into %s resource metrics. Investigate auto-scaling policies, " +
                    "unattached storage, or burst compute instances active on %s.",
                    serviceName, targetDate);

            AnomalyCandidate candidate = AnomalyCandidate.builder()
                    .cloudAccount(account)
                    .serviceName(serviceName)
                    .region(region)
                    .expectedCost(mean)
                    .actualCost(targetSpend)
                    .excessCost(excessCost)
                    .deviationPercentage(deviationPct)
                    .severity(severity)
                    .detectionMethod(AnomalyDetectionMethod.Z_SCORE)
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

