package com.cloudcostai.optimization.engine;

import com.cloudcostai.entity.CloudAccount;
import com.cloudcostai.entity.CloudProvider;
import com.cloudcostai.entity.OptimizationPriority;
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
class RightsizingEngineServiceTest {

    @Mock
    private ResourceUtilizationSnapshotRepository snapshotRepository;

    @Mock
    private BillingRecordRepository billingRecordRepository;

    @InjectMocks
    private RightsizingEngineService rightsizingEngineService;

    @Test
    @DisplayName("Should generate rightsizing recommendation for underutilized EC2 instance")
    void shouldGenerateRecommendationForUnderutilizedInstance() {
        Organization org = Organization.builder().name("Acme Corp").build();
        org.setId(1L);

        CloudAccount account = CloudAccount.builder()
                .organization(org)
                .provider(CloudProvider.AWS)
                .accountName("Production AWS")
                .accountId("123456789012")
                .build();
        account.setId(10L);

        ResourceUtilizationSnapshot snapshot = ResourceUtilizationSnapshot.builder()
                .organization(org)
                .cloudAccount(account)
                .resourceId("i-0abc1234def567890")
                .resourceName("api-server-prod-01")
                .resourceType("c5.2xlarge")
                .region("us-east-1")
                .cpuUtilizationAvg(new BigDecimal("8.50"))
                .cpuUtilizationMax(new BigDecimal("22.40"))
                .memoryUtilizationAvg(new BigDecimal("35.00"))
                .runningHours(new BigDecimal("730.00"))
                .snapshotDate(LocalDate.now().minusDays(1))
                .build();

        when(snapshotRepository.findByOrganizationIdOrderBySnapshotDateDesc(eq(1L), any(Pageable.class)))
                .thenReturn(List.of(snapshot));
        when(billingRecordRepository.sumCostByOrganizationIdAndResourceId(eq(1L), eq("i-0abc1234def567890")))
                .thenReturn(new BigDecimal("248.2000"));

        List<CandidateRecommendation> recommendations = rightsizingEngineService.evaluate(org);

        assertThat(recommendations).hasSize(1);
        CandidateRecommendation rec = recommendations.get(0);
        assertThat(rec.getResourceId()).isEqualTo("i-0abc1234def567890");
        assertThat(rec.getRecommendationType()).isEqualTo(OptimizationRecommendationType.RIGHTSIZING);
        assertThat(rec.getCurrentConfig()).isEqualTo("c5.2xlarge");
        assertThat(rec.getSuggestedConfig()).isEqualTo("c5.xlarge");
        assertThat(rec.getCurrentMonthlyCost()).isEqualByComparingTo("248.2000");
        assertThat(rec.getRecommendedMonthlyCost()).isEqualByComparingTo("124.1000");
        assertThat(rec.getEstimatedMonthlySavings()).isEqualByComparingTo("124.1000");
        assertThat(rec.getEstimatedAnnualSavings()).isEqualByComparingTo("1489.2000");
        assertThat(rec.getPriority()).isEqualTo(OptimizationPriority.HIGH);
    }

    @Test
    @DisplayName("Should skip rightsizing when CPU telemetry is missing (avoid unsupported fabrication)")
    void shouldSkipWhenTelemetryMissing() {
        Organization org = Organization.builder().name("Acme Corp").build();
        org.setId(1L);

        CloudAccount account = CloudAccount.builder()
                .organization(org)
                .provider(CloudProvider.AWS)
                .accountName("Production AWS")
                .accountId("123456789012")
                .build();

        ResourceUtilizationSnapshot snapshotWithoutCpu = ResourceUtilizationSnapshot.builder()
                .organization(org)
                .cloudAccount(account)
                .resourceId("i-no-telemetry")
                .resourceName("unmonitored-server")
                .resourceType("m5.large")
                .cpuUtilizationAvg(null) // Telemetry unavailable
                .cpuUtilizationMax(null)
                .snapshotDate(LocalDate.now())
                .build();

        when(snapshotRepository.findByOrganizationIdOrderBySnapshotDateDesc(eq(1L), any(Pageable.class)))
                .thenReturn(List.of(snapshotWithoutCpu));

        List<CandidateRecommendation> recommendations = rightsizingEngineService.evaluate(org);

        // Crucial requirement: Must not fabricate metrics or create unsupported rightsizing recommendations
        assertThat(recommendations).isEmpty();
    }
}

