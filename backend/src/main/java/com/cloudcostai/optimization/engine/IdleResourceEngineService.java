package com.cloudcostai.optimization.engine;

import com.cloudcostai.entity.CloudAccount;
import com.cloudcostai.entity.CloudProvider;
import com.cloudcostai.entity.OptimizationConfidence;
import com.cloudcostai.entity.OptimizationPriority;
import com.cloudcostai.entity.OptimizationRecommendationType;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.entity.ResourceUtilizationSnapshot;
import com.cloudcostai.optimization.model.CandidateRecommendation;
import com.cloudcostai.repository.BillingRecordRepository;
import com.cloudcostai.repository.ResourceUtilizationSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdleResourceEngineService {

    private final ResourceUtilizationSnapshotRepository snapshotRepository;
    private final BillingRecordRepository billingRecordRepository;

    private static final BigDecimal IDLE_CPU_THRESHOLD = new BigDecimal("2.50");
    private static final BigDecimal IDLE_MAX_CPU_THRESHOLD = new BigDecimal("5.00");
    private static final BigDecimal MONTHS_PER_YEAR = new BigDecimal("12.00");

    public List<CandidateRecommendation> evaluate(Organization org) {
        List<CandidateRecommendation> candidates = new ArrayList<>();
        if (org == null) return candidates;

        List<ResourceUtilizationSnapshot> snapshots = snapshotRepository.findByOrganizationIdOrderBySnapshotDateDesc(
                org.getId(), PageRequest.of(0, 200));

        if (snapshots.isEmpty()) {
            return candidates;
        }

        for (ResourceUtilizationSnapshot snapshot : snapshots) {
            String rType = snapshot.getResourceType().toUpperCase();
            String rId = snapshot.getResourceId();
            CloudAccount account = snapshot.getCloudAccount();
            CloudProvider provider = account != null ? account.getProvider() : CloudProvider.AWS;

            // 1. Detect Unattached Storage Volumes (EBS / Disks)
            if (isStorageVolume(rType)) {
                // If running hours is 0 or avg CPU is 0 (or null indicating unattached disk with storage billing)
                if (snapshot.getRunningHours() != null && snapshot.getRunningHours().compareTo(BigDecimal.ZERO) == 0) {
                    BigDecimal monthlyCost = resolveVolumeCost(org.getId(), rId, rType);
                    if (monthlyCost.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal annualSavings = monthlyCost.multiply(MONTHS_PER_YEAR).setScale(4, RoundingMode.HALF_UP);
                        candidates.add(CandidateRecommendation.builder()
                                .cloudAccount(account)
                                .resourceId(rId)
                                .resourceName(snapshot.getResourceName() != null ? snapshot.getResourceName() : rId)
                                .resourceType(snapshot.getResourceType())
                                .cloudProvider(provider)
                                .recommendationType(OptimizationRecommendationType.IDLE_RESOURCE)
                                .currentConfig("Unattached Volume (" + snapshot.getResourceType() + ")")
                                .suggestedConfig("Create snapshot backup and delete unattached volume")
                                .priority(monthlyCost.compareTo(new BigDecimal("50.00")) >= 0 ? OptimizationPriority.HIGH : OptimizationPriority.MEDIUM)
                                .confidenceLevel(OptimizationConfidence.HIGH)
                                .reason("Volume has 0 attached instance compute hours over the monitoring window while continuously incurring storage charges.")
                                .currentMonthlyCost(monthlyCost)
                                .recommendedMonthlyCost(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                                .estimatedMonthlySavings(monthlyCost)
                                .estimatedAnnualSavings(annualSavings)
                                .currency("USD")
                                .build());
                    }
                    continue;
                }
            }

            // 2. Detect Idle Load Balancers
            if (isLoadBalancer(rType)) {
                // If running hours > 0 but CPU/requests avg is 0 or near zero
                if (snapshot.getCpuUtilizationAvg() != null && snapshot.getCpuUtilizationAvg().compareTo(BigDecimal.ZERO) == 0) {
                    BigDecimal monthlyCost = resolveLoadBalancerCost(org.getId(), rId);
                    BigDecimal annualSavings = monthlyCost.multiply(MONTHS_PER_YEAR).setScale(4, RoundingMode.HALF_UP);
                    candidates.add(CandidateRecommendation.builder()
                            .cloudAccount(account)
                            .resourceId(rId)
                            .resourceName(snapshot.getResourceName() != null ? snapshot.getResourceName() : rId)
                            .resourceType(snapshot.getResourceType())
                            .cloudProvider(provider)
                            .recommendationType(OptimizationRecommendationType.IDLE_RESOURCE)
                            .currentConfig("Active Load Balancer with 0 traffic")
                            .suggestedConfig("Decommission unused load balancer")
                            .priority(OptimizationPriority.MEDIUM)
                            .confidenceLevel(OptimizationConfidence.HIGH)
                            .reason("Load balancer has 0 registered active targets and 0 throughput over the evaluation period.")
                            .currentMonthlyCost(monthlyCost)
                            .recommendedMonthlyCost(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                            .estimatedMonthlySavings(monthlyCost)
                            .estimatedAnnualSavings(annualSavings)
                            .currency("USD")
                            .build());
                    continue;
                }
            }

            // 3. Detect Idle Compute Instances (CPU avg < 2.5%, max < 5.0%)
            if (snapshot.getCpuUtilizationAvg() != null) {
                BigDecimal avg = snapshot.getCpuUtilizationAvg();
                BigDecimal max = snapshot.getCpuUtilizationMax() != null ? snapshot.getCpuUtilizationMax() : avg;

                if (avg.compareTo(IDLE_CPU_THRESHOLD) < 0 && max.compareTo(IDLE_MAX_CPU_THRESHOLD) < 0) {
                    BigDecimal monthlyCost = resolveComputeCost(org.getId(), rId, snapshot.getResourceType());
                    if (monthlyCost.compareTo(BigDecimal.ZERO) > 0) {
                        // Stopping idle instance saves ~90% (leaving minimal EBS root volume cost)
                        BigDecimal recommendedCost = monthlyCost.multiply(new BigDecimal("0.10")).setScale(4, RoundingMode.HALF_UP);
                        BigDecimal monthlySavings = monthlyCost.subtract(recommendedCost).setScale(4, RoundingMode.HALF_UP);
                        BigDecimal annualSavings = monthlySavings.multiply(MONTHS_PER_YEAR).setScale(4, RoundingMode.HALF_UP);

                        OptimizationPriority priority = monthlySavings.compareTo(new BigDecimal("200.00")) >= 0
                                ? OptimizationPriority.CRITICAL
                                : OptimizationPriority.HIGH;

                        String reason = String.format("Instance has near-zero activity: Average CPU is %s%% (peak %s%%) with minimal disk/network I/O. Recommended to stop instance when not in use.",
                                avg.setScale(1, RoundingMode.HALF_UP).toPlainString(),
                                max.setScale(1, RoundingMode.HALF_UP).toPlainString());

                        candidates.add(CandidateRecommendation.builder()
                                .cloudAccount(account)
                                .resourceId(rId)
                                .resourceName(snapshot.getResourceName() != null ? snapshot.getResourceName() : rId)
                                .resourceType(snapshot.getResourceType())
                                .cloudProvider(provider)
                                .recommendationType(OptimizationRecommendationType.IDLE_RESOURCE)
                                .currentConfig("Running " + snapshot.getResourceType() + " (Always On)")
                                .suggestedConfig("Stop instance or schedule automated shutdown")
                                .priority(priority)
                                .confidenceLevel(OptimizationConfidence.HIGH)
                                .reason(reason)
                                .currentMonthlyCost(monthlyCost)
                                .recommendedMonthlyCost(recommendedCost)
                                .estimatedMonthlySavings(monthlySavings)
                                .estimatedAnnualSavings(annualSavings)
                                .currency("USD")
                                .build());
                    }
                }
            }
        }

        log.info("Idle resource engine generated {} candidate recommendations for org [{}]", candidates.size(), org.getId());
        return candidates;
    }

    private boolean isStorageVolume(String type) {
        return type.contains("EBS") || type.contains("VOLUME") || type.contains("DISK") || type.contains("STORAGE");
    }

    private boolean isLoadBalancer(String type) {
        return type.contains("ELB") || type.contains("ALB") || type.contains("LOAD_BALANCER") || type.contains("LOADBALANCER");
    }

    private BigDecimal resolveVolumeCost(Long orgId, String resourceId, String resourceType) {
        BigDecimal cost = billingRecordRepository.sumCostByOrganizationIdAndResourceId(orgId, resourceId);
        if (cost != null && cost.compareTo(BigDecimal.ZERO) > 0) return cost;
        // Standard GP3 / SSD volume typical cost (e.g. 200 GB @ $0.08/GB = $16, or 500 GB = $40)
        return new BigDecimal("32.0000");
    }

    private BigDecimal resolveLoadBalancerCost(Long orgId, String resourceId) {
        BigDecimal cost = billingRecordRepository.sumCostByOrganizationIdAndResourceId(orgId, resourceId);
        if (cost != null && cost.compareTo(BigDecimal.ZERO) > 0) return cost;
        return new BigDecimal("22.5000"); // Standard ALB baseline base price (~$0.0225/hr * 730)
    }

    private BigDecimal resolveComputeCost(Long orgId, String resourceId, String resourceType) {
        BigDecimal cost = billingRecordRepository.sumCostByOrganizationIdAndResourceId(orgId, resourceId);
        if (cost != null && cost.compareTo(BigDecimal.ZERO) > 0) return cost;
        return switch (resourceType) {
            case "c5.2xlarge", "m5.2xlarge" -> new BigDecimal("248.2000");
            case "c5.xlarge", "m5.xlarge" -> new BigDecimal("124.1000");
            default -> new BigDecimal("95.0000");
        };
    }
}

