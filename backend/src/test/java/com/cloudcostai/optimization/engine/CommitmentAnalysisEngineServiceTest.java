package com.cloudcostai.optimization.engine;

import com.cloudcostai.dto.CommitmentAnalysisResponse;
import com.cloudcostai.entity.CloudAccount;
import com.cloudcostai.entity.CloudProvider;
import com.cloudcostai.entity.OptimizationRecommendationType;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.optimization.model.CandidateRecommendation;
import com.cloudcostai.repository.BillingRecordRepository;
import com.cloudcostai.repository.CloudAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommitmentAnalysisEngineServiceTest {

    @Mock
    private BillingRecordRepository billingRecordRepository;

    @Mock
    private CloudAccountRepository cloudAccountRepository;

    @InjectMocks
    private CommitmentAnalysisEngineService commitmentAnalysisEngineService;

    @Test
    @DisplayName("Should project 1-year and 3-year commitment savings based on compute baseline")
    void shouldProjectCommitmentSavings() {
        Organization org = Organization.builder().name("Acme Corp").build();
        org.setId(1L);

        CloudAccount account = CloudAccount.builder()
                .organization(org)
                .provider(CloudProvider.AWS)
                .accountName("Production AWS")
                .accountId("123456789012")
                .build();

        when(billingRecordRepository.sumComputeCostSince(eq(1L), any(LocalDate.class)))
                .thenReturn(new BigDecimal("1000.0000"));
        when(billingRecordRepository.findDailyComputeCostsSince(eq(1L), any(LocalDate.class)))
                .thenReturn(List.of());
        when(cloudAccountRepository.findByOrganizationId(eq(1L)))
                .thenReturn(List.of(account));

        CommitmentAnalysisResponse analysis = commitmentAnalysisEngineService.analyzeCommitment(org);

        assertThat(analysis).isNotNull();
        // Baseline = 1000 * 0.75 = 750
        assertThat(analysis.getBaselineMonthlyComputeSpend()).isEqualByComparingTo("750.0000");
        // 1-Yr Savings = 750 * 0.28 = 210.0000
        assertThat(analysis.getEstimated1YearMonthlySavings()).isEqualByComparingTo("210.0000");
        assertThat(analysis.getEstimated1YearAnnualSavings()).isEqualByComparingTo("2520.0000");
        // 3-Yr Savings = 750 * 0.42 = 315.0000
        assertThat(analysis.getEstimated3YearMonthlySavings()).isEqualByComparingTo("315.0000");
        assertThat(analysis.getEstimated3YearAnnualSavings()).isEqualByComparingTo("3780.0000");

        List<CandidateRecommendation> candidates = commitmentAnalysisEngineService.evaluate(org);
        assertThat(candidates).hasSize(1);
        CandidateRecommendation rec = candidates.get(0);
        assertThat(rec.getRecommendationType()).isEqualTo(OptimizationRecommendationType.COMMITMENT_SAVINGS_PLAN);
        assertThat(rec.getEstimatedMonthlySavings()).isEqualByComparingTo("210.0000");
    }
}

