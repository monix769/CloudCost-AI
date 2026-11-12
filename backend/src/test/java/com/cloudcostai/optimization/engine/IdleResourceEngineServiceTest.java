package com.cloudcostai.optimization.engine;

import com.cloudcostai.entity.CloudAccount;
import com.cloudcostai.entity.CloudProvider;
import com.cloudcostai.entity.OptimizationRecommendationType;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.entity.ResourceUtilizationSnapshot;
import com.cloudcostai.optimization.model.CandidateRecommendation;
import com.cloudcostai.repository.BillingRecordRepository;
import com.cloudcostai.repository.ResourceUtilizationSnapshotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdleResourceEngineServiceTest {

    @Mock
    private ResourceUtilizationSnapshotRepository snapshotRepository;

    @Mock
    private BillingRecordRepository billingRecordRepository;

    @InjectMocks
    private IdleResourceEngineService idleResourceEngineService;

    @Test
    @DisplayName("Should detect unattached storage volume with 0 running hours")
    void shouldDetectUnattachedStorageVolume() {
        Organization org = Organization.builder().name("Acme Corp").build();
        org.setId(1L);

        CloudAccount account = CloudAccount.builder()
                .organization(org)
                .provider(CloudProvider.AWS)
                .accountName("Production AWS")
                .accountId("123456789012")
                .build();

        ResourceUtilizationSnapshot unattachedVolume = ResourceUtilizationSnapshot.builder()
                .organization(org)
                .cloudAccount(account)
                .resourceId("vol-0123456789abcdef0")
                .resourceName("orphan-ebs-backup")
                .resourceType("EBS_VOLUME_GP3")
                .runningHours(BigDecimal.ZERO) // 0 attached hours
                .snapshotDate(LocalDate.now().minusDays(1))
                .build();

        when(snapshotRepository.findByOrganizationIdOrderBySnapshotDateDesc(eq(1L), any(Pageable.class)))
                .thenReturn(List.of(unattachedVolume));
        when(billingRecordRepository.sumCostByOrganizationIdAndResourceId(eq(1L), eq("vol-0123456789abcdef0")))
                .thenReturn(new BigDecimal("48.0000"));

        List<CandidateRecommendation> recommendations = idleResourceEngineService.evaluate(org);

        assertThat(recommendations).hasSize(1);
        CandidateRecommendation rec = recommendations.get(0);
        assertThat(rec.getResourceId()).isEqualTo("vol-0123456789abcdef0");
        assertThat(rec.getRecommendationType()).isEqualTo(OptimizationRecommendationType.IDLE_RESOURCE);
        assertThat(rec.getCurrentMonthlyCost()).isEqualByComparingTo("48.0000");
        assertThat(rec.getRecommendedMonthlyCost()).isEqualByComparingTo("0.0000");
        assertThat(rec.getEstimatedMonthlySavings()).isEqualByComparingTo("48.0000");
        assertThat(rec.getEstimatedAnnualSavings()).isEqualByComparingTo("576.0000");
    }

    @Test
    @DisplayName("Should detect idle instance with negligible CPU utilization")
    void shouldDetectIdleComputeInstance() {
        Organization org = Organization.builder().name("Acme Corp").build();
        org.setId(1L);

        CloudAccount account = CloudAccount.builder()
                .organization(org)
                .provider(CloudProvider.AWS)
                .accountName("Production AWS")
                .accountId("123456789012")
                .build();

        ResourceUtilizationSnapshot idleVm = ResourceUtilizationSnapshot.builder()
                .organization(org)
                .cloudAccount(account)
                .resourceId("i-abandoned-test-node")
                .resourceName("staging-qa-abandoned")
                .resourceType("m5.xlarge")
                .cpuUtilizationAvg(new BigDecimal("1.20")) // negligible
                .cpuUtilizationMax(new BigDecimal("3.40"))
                .runningHours(new BigDecimal("730.00"))
                .snapshotDate(LocalDate.now().minusDays(1))
                .build();

        when(snapshotRepository.findByOrganizationIdOrderBySnapshotDateDesc(eq(1L), any(Pageable.class)))
                .thenReturn(List.of(idleVm));
        when(billingRecordRepository.sumCostByOrganizationIdAndResourceId(eq(1L), eq("i-abandoned-test-node")))
                .thenReturn(new BigDecimal("124.1000"));

        List<CandidateRecommendation> recommendations = idleResourceEngineService.evaluate(org);

        assertThat(recommendations).hasSize(1);
        CandidateRecommendation rec = recommendations.get(0);
        assertThat(rec.getResourceId()).isEqualTo("i-abandoned-test-node");
        assertThat(rec.getRecommendationType()).isEqualTo(OptimizationRecommendationType.IDLE_RESOURCE);
        // Stopping instance retains ~10% for disk preservation
        assertThat(rec.getRecommendedMonthlyCost()).isEqualByComparingTo("12.4100");
        assertThat(rec.getEstimatedMonthlySavings()).isEqualByComparingTo("111.6900");
    }
}

