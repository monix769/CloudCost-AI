package com.cloudcostai.service;

import com.cloudcostai.dto.CostAllocationDimension;
import com.cloudcostai.dto.CostAllocationResponse;
import com.cloudcostai.dto.CostSummaryResponse;
import com.cloudcostai.dto.MonthOverMonthResponse;
import com.cloudcostai.dto.ProviderCostResponse;
import com.cloudcostai.dto.ServiceCostResponse;
import com.cloudcostai.dto.SpendingTrendsSeriesResponse;
import com.cloudcostai.dto.TimeGranularity;
import com.cloudcostai.entity.CloudProvider;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.exception.BadRequestException;
import com.cloudcostai.repository.BillingRecordRepository;
import com.cloudcostai.repository.CloudAccountRepository;
import com.cloudcostai.repository.projection.DailyCostSummary;
import com.cloudcostai.repository.projection.ProviderCostSummary;
import com.cloudcostai.repository.projection.ServiceCostSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdvancedCostAnalyticsServiceTest {

    @Mock
    private BillingRecordRepository billingRecordRepository;

    @Mock
    private CloudAccountRepository cloudAccountRepository;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private BudgetService budgetService;

    @InjectMocks
    private CostAnalyticsService costAnalyticsService;

    private Organization testOrg;

    @BeforeEach
    void setUp() {
        testOrg = Organization.builder().name("Test Org").slug("test-org").build();
        testOrg.setId(5L);
    }

    @Test
    @DisplayName("Should calculate cost summary with highest and lowest spending days")
    void shouldCalculateCostSummaryWithExtremes() {
        when(organizationService.getCurrentUserOrganization()).thenReturn(testOrg);
        when(billingRecordRepository.findDistinctCurrenciesByOrganizationId(5L)).thenReturn(List.of("USD"));
        when(billingRecordRepository.calculateTotalCost(eq(5L), eq("USD"), any(), any()))
                .thenReturn(BigDecimal.valueOf(15000.00));
        when(billingRecordRepository.countRecords(eq(5L), eq("USD"), any(), any()))
                .thenReturn(120L);

        List<DailyCostSummary> daily = new ArrayList<>();
        daily.add(new MockDailyCost(LocalDate.of(2026, 11, 1), BigDecimal.valueOf(300.00)));
        daily.add(new MockDailyCost(LocalDate.of(2026, 11, 2), BigDecimal.valueOf(1200.00))); // highest
        daily.add(new MockDailyCost(LocalDate.of(2026, 11, 3), BigDecimal.valueOf(150.00))); // lowest

        when(billingRecordRepository.aggregateDailyCosts(eq(5L), eq("USD"), any(), any())).thenReturn(daily);

        CostSummaryResponse response = costAnalyticsService.getCostSummary(
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 3),
                "USD");

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(15000.00).setScale(2), response.getTotalCost());
        assertEquals("USD", response.getCurrency());
        assertEquals(LocalDate.of(2026, 11, 2), response.getHighestSpendingDate());
        assertEquals(BigDecimal.valueOf(1200.00).setScale(2), response.getHighestSpendingDayCost());
        assertEquals(LocalDate.of(2026, 11, 3), response.getLowestSpendingDate());
        assertEquals(BigDecimal.valueOf(150.00).setScale(2), response.getLowestSpendingDayCost());
    }

    @Test
    @DisplayName("Should aggregate weekly spending trends with percentage change calculation")
    void shouldAggregateWeeklyTrends() {
        when(organizationService.getCurrentUserOrganization()).thenReturn(testOrg);
        when(billingRecordRepository.findDistinctCurrenciesByOrganizationId(5L)).thenReturn(List.of("USD"));

        List<DailyCostSummary> dailyList = new ArrayList<>();
        // Week 1 (Oct 26 - Nov 1)
        dailyList.add(new MockDailyCost(LocalDate.of(2026, 10, 26), BigDecimal.valueOf(200.00)));
        dailyList.add(new MockDailyCost(LocalDate.of(2026, 10, 27), BigDecimal.valueOf(300.00)));
        // Week 2 (Nov 2 - Nov 8)
        dailyList.add(new MockDailyCost(LocalDate.of(2026, 11, 2), BigDecimal.valueOf(500.00)));
        dailyList.add(new MockDailyCost(LocalDate.of(2026, 11, 3), BigDecimal.valueOf(500.00)));

        when(billingRecordRepository.aggregateDailyCosts(eq(5L), eq("USD"), any(), any())).thenReturn(dailyList);

        SpendingTrendsSeriesResponse series = costAnalyticsService.getSpendingTrends(
                TimeGranularity.WEEKLY,
                LocalDate.of(2026, 10, 26),
                LocalDate.of(2026, 11, 8),
                "USD",
                null);

        assertNotNull(series);
        assertEquals(TimeGranularity.WEEKLY, series.getGranularity());
        assertEquals(2, series.getPoints().size());
        assertEquals(BigDecimal.valueOf(1500.00).setScale(2), series.getTotalSpend());
        // Week 2 had 1000 spend vs week 1 with 500 spend => +100% change
        assertEquals(BigDecimal.valueOf(100.00).setScale(2), series.getPoints().get(1).getPercentageChange());
    }

    @Test
    @DisplayName("Should handle Month-over-Month calculation with zero previous month expenditure safely")
    void shouldHandleMonthOverMonthZeroPreviousSpend() {
        when(organizationService.getCurrentUserOrganization()).thenReturn(testOrg);
        when(billingRecordRepository.findDistinctCurrenciesByOrganizationId(5L)).thenReturn(List.of("USD"));

        // Current month $4,500, previous month $0.00
        when(billingRecordRepository.calculateTotalCost(eq(5L), eq("USD"), any(), any()))
                .thenReturn(BigDecimal.valueOf(4500.00))
                .thenReturn(BigDecimal.ZERO);

        when(billingRecordRepository.aggregateDailyCostsByProvider(any(), any(), any(), any(), any())).thenReturn(List.of());
        when(billingRecordRepository.aggregateCostByService(any(), any(), any(), any())).thenReturn(List.of());

        MonthOverMonthResponse mom = costAnalyticsService.getMonthOverMonthComparison("USD");

        assertNotNull(mom);
        assertEquals(BigDecimal.valueOf(4500.00).setScale(2), mom.getCurrentMonthCost());
        assertEquals(BigDecimal.ZERO.setScale(2), mom.getPreviousMonthCost());
        assertEquals("INCREASE", mom.getDirection());
        assertEquals(BigDecimal.valueOf(100.00).setScale(2), mom.getPercentageChange());
    }

    @Test
    @DisplayName("Should reject cross-currency aggregation when multiple distinct currencies exist and none specified")
    void shouldRejectMixedCurrenciesWithoutFilter() {
        when(organizationService.getCurrentUserOrganization()).thenReturn(testOrg);
        when(billingRecordRepository.findDistinctCurrenciesByOrganizationId(5L)).thenReturn(List.of("USD", "EUR"));

        assertThrows(BadRequestException.class, () -> costAnalyticsService.getCostSummary(null, null, null));
    }

    @Test
    @DisplayName("Should calculate cost allocation across dimensions")
    void shouldCalculateCostAllocation() {
        when(organizationService.getCurrentUserOrganization()).thenReturn(testOrg);
        when(billingRecordRepository.findDistinctCurrenciesByOrganizationId(5L)).thenReturn(List.of("USD"));
        when(billingRecordRepository.calculateTotalCost(eq(5L), eq("USD"), any(), any()))
                .thenReturn(BigDecimal.valueOf(10000.00));

        List<ProviderCostSummary> provSummaries = List.of(new ProviderCostSummary() {
            @Override
            public CloudProvider getProvider() { return CloudProvider.AWS; }
            @Override
            public BigDecimal getTotalCost() { return BigDecimal.valueOf(7000.00); }
            @Override
            public Long getRecordCount() { return 50L; }
        });

        when(billingRecordRepository.aggregateCostByProvider(eq(5L), eq("USD"), any(), any())).thenReturn(provSummaries);

        CostAllocationResponse allocation = costAnalyticsService.getCostAllocation(
                CostAllocationDimension.PROVIDER, null, null, "USD");

        assertNotNull(allocation);
        assertEquals(CostAllocationDimension.PROVIDER, allocation.getDimension());
        assertEquals(1, allocation.getItems().size());
        assertEquals("AWS", allocation.getItems().get(0).getKey());
        assertEquals(BigDecimal.valueOf(70.00).setScale(2), allocation.getItems().get(0).getPercentage());
    }

    private static class MockDailyCost implements DailyCostSummary {
        private final LocalDate date;
        private final BigDecimal cost;

        public MockDailyCost(LocalDate date, BigDecimal cost) {
            this.date = date;
            this.cost = cost;
        }

        @Override public LocalDate getUsageDate() { return date; }
        @Override public BigDecimal getTotalCost() { return cost; }
        @Override public Long getRecordCount() { return 5L; }
    }
}

