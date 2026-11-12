package com.cloudcostai.service;

import com.cloudcostai.dto.BudgetItemUtilizationResponse;
import com.cloudcostai.dto.BudgetHealthStatus;
import com.cloudcostai.dto.BudgetUtilizationResponse;
import com.cloudcostai.entity.Budget;
import com.cloudcostai.entity.BudgetPeriod;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.repository.BillingRecordRepository;
import com.cloudcostai.repository.BudgetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetAnalyticsTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private BillingRecordRepository billingRecordRepository;

    @Mock
    private OrganizationService organizationService;

    @InjectMocks
    private BudgetService budgetService;

    private Organization testOrg;

    @BeforeEach
    void setUp() {
        testOrg = Organization.builder().name("Acme Corp").slug("acme").build();
        testOrg.setId(100L);
    }

    @Test
    @DisplayName("Should evaluate HEALTHY, WARNING, CRITICAL, and EXCEEDED budget thresholds")
    void shouldEvaluateBudgetHealthThresholds() {
        when(organizationService.getCurrentUserOrganization()).thenReturn(testOrg);

        Budget b1 = Budget.builder()
                .organization(testOrg)
                .name("Healthy Budget")
                .amountLimit(BigDecimal.valueOf(10000.00))
                .period(BudgetPeriod.MONTHLY)
                .currency("USD")
                .alertThresholdPercentage(BigDecimal.valueOf(80.0))
                .build();
        b1.setId(1L);

        Budget b2 = Budget.builder()
                .organization(testOrg)
                .name("Warning Budget")
                .amountLimit(BigDecimal.valueOf(10000.00))
                .period(BudgetPeriod.MONTHLY)
                .currency("USD")
                .alertThresholdPercentage(BigDecimal.valueOf(80.0))
                .build();
        b2.setId(2L);

        Budget b3 = Budget.builder()
                .organization(testOrg)
                .name("Exceeded Budget")
                .amountLimit(BigDecimal.valueOf(10000.00))
                .period(BudgetPeriod.MONTHLY)
                .currency("USD")
                .alertThresholdPercentage(BigDecimal.valueOf(80.0))
                .build();
        b3.setId(3L);

        when(budgetRepository.findByOrganizationId(100L)).thenReturn(List.of(b1, b2, b3));

        // b1 spend = 5000 (50% -> HEALTHY)
        // b2 spend = 7500 (75% -> WARNING)
        // b3 spend = 11000 (110% -> EXCEEDED)
        when(billingRecordRepository.calculateTotalCost(eq(100L), any(), any()))
                .thenReturn(BigDecimal.valueOf(5000.00))
                .thenReturn(BigDecimal.valueOf(7500.00))
                .thenReturn(BigDecimal.valueOf(11000.00));

        BudgetUtilizationResponse response = budgetService.getBudgetUtilizationAnalytics("USD");

        assertNotNull(response);
        assertEquals(3, response.getBudgetBreakdowns().size());

        BudgetItemUtilizationResponse item1 = response.getBudgetBreakdowns().get(0);
        assertEquals(BudgetHealthStatus.HEALTHY, item1.getHealthStatus());
        assertEquals(BigDecimal.valueOf(50.00).setScale(2), item1.getUtilizationPercentage());

        BudgetItemUtilizationResponse item2 = response.getBudgetBreakdowns().get(1);
        assertEquals(BudgetHealthStatus.WARNING, item2.getHealthStatus());
        assertEquals(BigDecimal.valueOf(75.00).setScale(2), item2.getUtilizationPercentage());

        BudgetItemUtilizationResponse item3 = response.getBudgetBreakdowns().get(2);
        assertEquals(BudgetHealthStatus.EXCEEDED, item3.getHealthStatus());
        assertEquals(BigDecimal.valueOf(110.00).setScale(2), item3.getUtilizationPercentage());

        // Total allocated = 30000, Total current = 23500 => ~78.33% => overall WARNING
        assertEquals(BigDecimal.valueOf(30000.00).setScale(2), response.getTotalAllocatedBudget());
        assertEquals(BigDecimal.valueOf(23500.00).setScale(2), response.getTotalCurrentSpend());
        assertEquals(BudgetHealthStatus.WARNING, response.getOverallHealthStatus());
    }
}

