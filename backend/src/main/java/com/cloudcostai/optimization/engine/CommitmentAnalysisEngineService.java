package com.cloudcostai.optimization.engine;

import com.cloudcostai.dto.CommitmentAnalysisResponse;
import com.cloudcostai.entity.CloudAccount;
import com.cloudcostai.entity.CloudProvider;
import com.cloudcostai.entity.OptimizationConfidence;
import com.cloudcostai.entity.OptimizationPriority;
import com.cloudcostai.entity.OptimizationRecommendationType;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.optimization.model.CandidateRecommendation;
import com.cloudcostai.repository.BillingRecordRepository;
import com.cloudcostai.repository.CloudAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommitmentAnalysisEngineService {

    private final BillingRecordRepository billingRecordRepository;
    private final CloudAccountRepository cloudAccountRepository;

    private static final BigDecimal SAVINGS_PLAN_1YR_DISCOUNT = new BigDecimal("0.28"); // 28%
    private static final BigDecimal SAVINGS_PLAN_3YR_DISCOUNT = new BigDecimal("0.42"); // 42%
    private static final BigDecimal COVERAGE_RATIO = new BigDecimal("0.75"); // 75% conservative baseline coverage
    private static final BigDecimal MONTHS_PER_YEAR = new BigDecimal("12.00");

    public CommitmentAnalysisResponse analyzeCommitment(Organization org) {
        if (org == null) {
            return CommitmentAnalysisResponse.builder()
                    .evaluatedDays(30)
                    .totalComputeSpend(BigDecimal.ZERO)
                    .baselineMonthlyComputeSpend(BigDecimal.ZERO)
                    .currency("USD")
                    .assumptions(List.of("No data available"))
                    .build();
        }

        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        BigDecimal totalComputeSpend = billingRecordRepository.sumComputeCostSince(org.getId(), thirtyDaysAgo);
        if (totalComputeSpend == null) totalComputeSpend = BigDecimal.ZERO;

        List<Object[]> dailyComputeCosts = billingRecordRepository.findDailyComputeCostsSince(org.getId(), thirtyDaysAgo);

        BigDecimal baselineMonthlySpend;
        if (!dailyComputeCosts.isEmpty()) {
            // Find minimum non-zero daily spend or average daily spend
            BigDecimal minDaily = BigDecimal.valueOf(Double.MAX_VALUE);
            BigDecimal sumDaily = BigDecimal.ZERO;
            for (Object[] row : dailyComputeCosts) {
                BigDecimal dCost = (BigDecimal) row[1];
                if (dCost != null && dCost.compareTo(BigDecimal.ZERO) > 0) {
                    if (dCost.compareTo(minDaily) < 0) minDaily = dCost;
                    sumDaily = sumDaily.add(dCost);
                }
            }
            if (minDaily.compareTo(BigDecimal.valueOf(Double.MAX_VALUE)) == 0) {
                minDaily = sumDaily.divide(BigDecimal.valueOf(Math.max(1, dailyComputeCosts.size())), 4, RoundingMode.HALF_UP);
            }
            // Conservative baseline monthly compute = min daily * 30 * coverage ratio
            baselineMonthlySpend = minDaily.multiply(new BigDecimal("30.00")).multiply(COVERAGE_RATIO).setScale(4, RoundingMode.HALF_UP);
        } else {
            // Fallback based on total compute spend
            baselineMonthlySpend = totalComputeSpend.multiply(COVERAGE_RATIO).setScale(4, RoundingMode.HALF_UP);
        }

        // 1-Year Plan
        BigDecimal savings1YrMonthly = baselineMonthlySpend.multiply(SAVINGS_PLAN_1YR_DISCOUNT).setScale(4, RoundingMode.HALF_UP);
        BigDecimal cost1YrMonthly = baselineMonthlySpend.subtract(savings1YrMonthly).setScale(4, RoundingMode.HALF_UP);
        BigDecimal savings1YrAnnual = savings1YrMonthly.multiply(MONTHS_PER_YEAR).setScale(4, RoundingMode.HALF_UP);

        // 3-Year Plan
        BigDecimal savings3YrMonthly = baselineMonthlySpend.multiply(SAVINGS_PLAN_3YR_DISCOUNT).setScale(4, RoundingMode.HALF_UP);
        BigDecimal cost3YrMonthly = baselineMonthlySpend.subtract(savings3YrMonthly).setScale(4, RoundingMode.HALF_UP);
        BigDecimal savings3YrAnnual = savings3YrMonthly.multiply(MONTHS_PER_YEAR).setScale(4, RoundingMode.HALF_UP);

        List<String> assumptions = Arrays.asList(
                "Evaluated continuous 30-day compute expenditure across EC2, Azure VMs, and GCP Compute Engine.",
                "Conservative 75% coverage target applied to stable baseline to minimize over-commitment risk.",
                "Assumes 1-Year / 3-Year No-Upfront Compute Savings Plans or Standard Reserved Instances.",
                "Prices calculated relative to on-demand retail rates without custom enterprise agreement discounts."
        );

        return CommitmentAnalysisResponse.builder()
                .evaluatedDays(30)
                .totalComputeSpend(totalComputeSpend)
                .baselineMonthlyComputeSpend(baselineMonthlySpend)
                .recommended1YearSavingsPlanMonthly(cost1YrMonthly)
                .estimated1YearSavingsPercent(new BigDecimal("28.00"))
                .estimated1YearMonthlySavings(savings1YrMonthly)
                .estimated1YearAnnualSavings(savings1YrAnnual)
                .recommended3YearSavingsPlanMonthly(cost3YrMonthly)
                .estimated3YearSavingsPercent(new BigDecimal("42.00"))
                .estimated3YearMonthlySavings(savings3YrMonthly)
                .estimated3YearAnnualSavings(savings3YrAnnual)
                .assumptions(assumptions)
                .currency("USD")
                .build();
    }

    public List<CandidateRecommendation> evaluate(Organization org) {
        List<CandidateRecommendation> candidates = new ArrayList<>();
        if (org == null) return candidates;

        CommitmentAnalysisResponse analysis = analyzeCommitment(org);
        if (analysis.getBaselineMonthlyComputeSpend().compareTo(new BigDecimal("100.00")) < 0) {
            log.debug("Baseline compute spend ${} is below $100/mo threshold for commitment recommendations",
                    analysis.getBaselineMonthlyComputeSpend());
            return candidates;
        }

        // Pick primary cloud account
        List<CloudAccount> accounts = cloudAccountRepository.findByOrganizationId(org.getId());
        CloudAccount primaryAccount = accounts.isEmpty() ? null : accounts.get(0);
        CloudProvider provider = primaryAccount != null ? primaryAccount.getProvider() : CloudProvider.AWS;

        String planName = switch (provider) {
            case AWS -> "AWS Compute Savings Plan (1-Year No-Upfront)";
            case AZURE -> "Azure 1-Year Reserved Virtual Machine Instances";
            case GCP -> "GCP 1-Year Committed Use Discount (CUD)";
        };

        String reason = String.format("Analyzed 30-day baseline compute expenditure is $%s/mo. Committing to a 1-Year plan covering 75%% of baseline unlocks a guaranteed 28%% discount without upfront cash commitment.",
                analysis.getBaselineMonthlyComputeSpend().setScale(2, RoundingMode.HALF_UP).toPlainString());

        candidates.add(CandidateRecommendation.builder()
                .cloudAccount(primaryAccount)
                .resourceId("SP-BASELINE-COMPUTE")
                .resourceName("Compute Fleet Baseline Commitment")
                .resourceType("COMPUTE_SAVINGS_PLAN")
                .cloudProvider(provider)
                .recommendationType(OptimizationRecommendationType.COMMITMENT_SAVINGS_PLAN)
                .currentConfig("On-Demand Compute Fleet ($" + analysis.getBaselineMonthlyComputeSpend().setScale(2, RoundingMode.HALF_UP) + "/mo)")
                .suggestedConfig(planName)
                .priority(OptimizationPriority.HIGH)
                .confidenceLevel(OptimizationConfidence.HIGH)
                .reason(reason)
                .currentMonthlyCost(analysis.getBaselineMonthlyComputeSpend())
                .recommendedMonthlyCost(analysis.getRecommended1YearSavingsPlanMonthly())
                .estimatedMonthlySavings(analysis.getEstimated1YearMonthlySavings())
                .estimatedAnnualSavings(analysis.getEstimated1YearAnnualSavings())
                .currency("USD")
                .build());

        log.info("Commitment engine generated {} candidate recommendations for org [{}]", candidates.size(), org.getId());
        return candidates;
    }
}

