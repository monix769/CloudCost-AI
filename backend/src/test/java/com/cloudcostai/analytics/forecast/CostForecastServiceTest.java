package com.cloudcostai.analytics.forecast;

import com.cloudcostai.entity.Organization;
import com.cloudcostai.repository.BillingRecordRepository;
import com.cloudcostai.repository.projection.DailyCostSummary;
import com.cloudcostai.service.OrganizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class CostForecastServiceTest {

    @Mock
    private BillingRecordRepository billingRecordRepository;

    @Mock
    private OrganizationService organizationService;

    private MovingAverageForecaster movingAverageForecaster;
    private ExponentialSmoothingForecaster exponentialSmoothingForecaster;
    private CostForecastService costForecastService;

    private Organization testOrg;

    @BeforeEach
    void setUp() {
        movingAverageForecaster = new MovingAverageForecaster();
        exponentialSmoothingForecaster = new ExponentialSmoothingForecaster();

        List<Forecaster> forecasters = List.of(movingAverageForecaster, exponentialSmoothingForecaster);
        costForecastService = new CostForecastService(billingRecordRepository, organizationService, forecasters);
        costForecastService.init();

        testOrg = Organization.builder().name("Acme Cloud Corp").slug("acme").build();
        testOrg.setId(1L);
    }

    private List<DailyCostSummary> createMockDailySummaries(int days, double startCost, double increment) {
        List<DailyCostSummary> list = new ArrayList<>();
        LocalDate start = LocalDate.now().minusDays(days);
        for (int i = 0; i < days; i++) {
            final LocalDate date = start.plusDays(i);
            final BigDecimal cost = BigDecimal.valueOf(startCost + (i * increment));
            list.add(new DailyCostSummary() {
                @Override
                public LocalDate getUsageDate() { return date; }
                @Override
                public BigDecimal getTotalCost() { return cost; }
                @Override
                public Long getRecordCount() { return 10L; }
            });
        }
        return list;
    }

    @Test
    @DisplayName("Should generate 7-day Moving Average forecast correctly")
    void shouldGenerate7DayMovingAverageForecast() {
        when(organizationService.getCurrentUserOrganization()).thenReturn(testOrg);
        when(billingRecordRepository.findDistinctCurrenciesByOrganizationId(1L)).thenReturn(List.of("USD"));
        when(billingRecordRepository.aggregateDailyCosts(eq(1L), eq("USD"), any(), any()))
                .thenReturn(createMockDailySummaries(30, 100.0, 5.0));

        ForecastResult result = costForecastService.generateForecast(
                ForecastHorizon.DAYS_7,
                ForecastMethod.MOVING_AVERAGE,
                null,
                14,
                "USD");

        assertNotNull(result);
        assertEquals(ForecastMethod.MOVING_AVERAGE, result.getMethod());
        assertEquals("7 Days", result.getHorizon());
        assertEquals("USD", result.getCurrency());
        assertEquals(7, result.getForecastData().size());
        assertTrue(result.getAverageDailyForecastCost().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(result.getEstimatedTotalForecastCost().compareTo(BigDecimal.ZERO) > 0);
        assertNotNull(result.getForecastData().get(0).getLowerBound());
        assertNotNull(result.getForecastData().get(0).getUpperBound());
    }

    @Test
    @DisplayName("Should generate 30-day and 90-day Exponential Smoothing forecast with expanding uncertainty")
    void shouldGenerateExponentialSmoothingForecast() {
        when(organizationService.getCurrentUserOrganization()).thenReturn(testOrg);
        when(billingRecordRepository.findDistinctCurrenciesByOrganizationId(1L)).thenReturn(List.of("USD"));
        when(billingRecordRepository.aggregateDailyCosts(eq(1L), eq("USD"), any(), any()))
                .thenReturn(createMockDailySummaries(60, 500.0, 10.0));

        ForecastResult result30 = costForecastService.generateForecast(
                ForecastHorizon.DAYS_30,
                ForecastMethod.EXPONENTIAL_SMOOTHING,
                0.35,
                null,
                "USD");

        assertNotNull(result30);
        assertEquals(ForecastMethod.EXPONENTIAL_SMOOTHING, result30.getMethod());
        assertEquals(30, result30.getForecastData().size());
        assertEquals(0.35, result30.getAlpha());

        // Verify statistical confidence interval widens over the horizon
        ForecastDataPoint day1 = result30.getForecastData().get(0);
        ForecastDataPoint day30 = result30.getForecastData().get(29);
        double spreadDay1 = day1.getUpperBound().subtract(day1.getLowerBound()).doubleValue();
        double spreadDay30 = day30.getUpperBound().subtract(day30.getLowerBound()).doubleValue();
        assertTrue(spreadDay30 > spreadDay1, "Confidence interval spread should widen over time");
    }

    @Test
    @DisplayName("Should handle insufficient historical data gracefully")
    void shouldHandleInsufficientHistoricalDataGracefully() {
        when(organizationService.getCurrentUserOrganization()).thenReturn(testOrg);
        when(billingRecordRepository.findDistinctCurrenciesByOrganizationId(1L)).thenReturn(List.of("USD"));
        when(billingRecordRepository.aggregateDailyCosts(eq(1L), eq("USD"), any(), any()))
                .thenReturn(new ArrayList<>());

        ForecastResult result = costForecastService.generateForecast(
                ForecastHorizon.DAYS_30,
                ForecastMethod.MOVING_AVERAGE,
                null,
                14,
                "USD");

        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getEstimatedTotalForecastCost());
        assertTrue(result.getForecastData().isEmpty());
        assertTrue(result.getStatusMessage().toLowerCase().contains("insufficient"));
    }

    @Test
    @DisplayName("ForecastHorizon should parse string abbreviations robustly")
    void shouldParseHorizonStrings() {
        assertEquals(ForecastHorizon.DAYS_7, ForecastHorizon.fromString("7D"));
        assertEquals(ForecastHorizon.DAYS_7, ForecastHorizon.fromString("7_DAYS"));
        assertEquals(ForecastHorizon.DAYS_30, ForecastHorizon.fromString("30d"));
        assertEquals(ForecastHorizon.DAYS_90, ForecastHorizon.fromString("90D"));
        assertEquals(ForecastHorizon.DAYS_30, ForecastHorizon.fromString(null));
    }
}

