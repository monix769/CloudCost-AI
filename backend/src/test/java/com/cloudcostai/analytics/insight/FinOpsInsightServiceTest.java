package com.cloudcostai.analytics.insight;

import com.cloudcostai.dto.BudgetHealthStatus;
import com.cloudcostai.dto.BudgetResponse;
import com.cloudcostai.entity.CloudAccount;
import com.cloudcostai.entity.CloudProvider;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.repository.BillingRecordRepository;
import com.cloudcostai.repository.CloudAccountRepository;
import com.cloudcostai.repository.projection.AccountCostSummary;
import com.cloudcostai.repository.projection.ProviderCostSummary;
import com.cloudcostai.repository.projection.ServiceCostSummary;
import com.cloudcostai.service.BudgetService;
import com.cloudcostai.service.OrganizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinOpsInsightServiceTest {

    @Mock
    private BillingRecordRepository billingRecordRepository;

    @Mock
    private CloudAccountRepository cloudAccountRepository;

    @Mock
    private BudgetService budgetService;

    @Mock
    private OrganizationService organizationService;

    @InjectMocks
    private FinOpsInsightService finOpsInsightService;

    private Organization testOrg;

    @BeforeEach
    void setUp() {
        testOrg = Organization.builder().name("FinOps Global Corp").slug("finops-global").build();
        testOrg.setId(10L);
    }

    @Test
    @DisplayName("Should detect month-over-month expenditure spike")
    void shouldDetectMonthOverMonthSpike() {
        when(organizationService.getCurrentUserOrganization()).thenReturn(testOrg);
        when(billingRecordRepository.findDistinctCurrenciesByOrganizationId(10L)).thenReturn(List.of("USD"));

        // Current month $20,000, Prev month $10,000 (+100% spike)
        when(billingRecordRepository.calculateTotalCost(eq(10L), eq("USD"), any(), any()))
                .thenReturn(BigDecimal.valueOf(20000.00))
                .thenReturn(BigDecimal.valueOf(10000.00))
                .thenReturn(BigDecimal.valueOf(30000.00)); // total

        when(budgetService.getAllBudgets()).thenReturn(List.of());
        when(cloudAccountRepository.findByOrganizationId(10L)).thenReturn(List.of());

        List<FinOpsInsight> insights = finOpsInsightService.generateInsights("USD");

        assertFalse(insights.isEmpty());
        assertTrue(insights.stream().anyMatch(i -> i.getCategory().equals("COST_SPIKE")));
        FinOpsInsight spikeInsight = insights.stream()
                .filter(i -> i.getCategory().equals("COST_SPIKE"))
                .findFirst()
                .orElse(null);
        assertNotNull(spikeInsight);
        assertEquals(InsightSeverity.CRITICAL, spikeInsight.getSeverity());
        assertEquals(BigDecimal.valueOf(10000.00).setScale(2), spikeInsight.getFinancialImpact());
    }

    @Test
    @DisplayName("Should detect budget exceeded critical risk")
    void shouldDetectBudgetExceededRisk() {
        when(organizationService.getCurrentUserOrganization()).thenReturn(testOrg);
        when(billingRecordRepository.findDistinctCurrenciesByOrganizationId(10L)).thenReturn(List.of("USD"));
        when(billingRecordRepository.calculateTotalCost(any(), any(), any(), any())).thenReturn(BigDecimal.ZERO);

        BudgetResponse exceededBudget = BudgetResponse.builder()
                .id(1L)
                .name("Production EC2 Budget")
                .amountLimit(BigDecimal.valueOf(5000.00))
                .currentSpend(BigDecimal.valueOf(6200.00))
                .currency("USD")
                .alertThresholdPercentage(BigDecimal.valueOf(80.0))
                .status("ACTIVE")
                .build();

        when(budgetService.getAllBudgets()).thenReturn(List.of(exceededBudget));
        when(cloudAccountRepository.findByOrganizationId(10L)).thenReturn(List.of());

        List<FinOpsInsight> insights = finOpsInsightService.generateInsights("USD");

        assertTrue(insights.stream().anyMatch(i -> i.getSeverity() == InsightSeverity.CRITICAL && i.getCategory().equals("BUDGET_RISK")));
    }

    @Test
    @DisplayName("Should detect stale cloud account synchronization")
    void shouldDetectStaleSync() {
        when(organizationService.getCurrentUserOrganization()).thenReturn(testOrg);
        when(billingRecordRepository.findDistinctCurrenciesByOrganizationId(10L)).thenReturn(List.of("USD"));
        when(billingRecordRepository.calculateTotalCost(any(), any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(budgetService.getAllBudgets()).thenReturn(List.of());

        CloudAccount staleAccount = CloudAccount.builder()
                .accountName("Legacy Azure Workload")
                .provider(CloudProvider.AZURE)
                .syncStatus("FAILED")
                .lastSyncedAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .isActive(true)
                .build();
        staleAccount.setId(99L);

        when(cloudAccountRepository.findByOrganizationId(10L)).thenReturn(List.of(staleAccount));

        List<FinOpsInsight> insights = finOpsInsightService.generateInsights("USD");

        assertTrue(insights.stream().anyMatch(i -> i.getTitle().contains("Stale Billing Synchronization")));
    }
}

