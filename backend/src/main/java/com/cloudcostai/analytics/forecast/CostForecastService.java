package com.cloudcostai.analytics.forecast;

import com.cloudcostai.entity.Organization;
import com.cloudcostai.exception.BadRequestException;
import com.cloudcostai.repository.BillingRecordRepository;
import com.cloudcostai.repository.projection.DailyCostSummary;
import com.cloudcostai.service.OrganizationService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CostForecastService {

    private final BillingRecordRepository billingRecordRepository;
    private final OrganizationService organizationService;
    private final List<Forecaster> forecastersList;

    private final Map<ForecastMethod, Forecaster> forecasterMap = new EnumMap<>(ForecastMethod.class);

    @PostConstruct
    public void init() {
        for (Forecaster forecaster : forecastersList) {
            forecasterMap.put(forecaster.getMethod(), forecaster);
        }
        log.info("CostForecastService registered forecasters: {}", forecasterMap.keySet());
    }

    @Transactional(readOnly = true)
    public ForecastResult generateForecast(
            ForecastHorizon horizon,
            ForecastMethod method,
            Double alpha,
            Integer windowSize,
            String requestedCurrency) {

        Organization org = organizationService.getCurrentUserOrganization();
        String activeCurrency = resolveAndValidateCurrency(org.getId(), requestedCurrency);

        ForecastMethod activeMethod = (method != null) ? method : ForecastMethod.EXPONENTIAL_SMOOTHING;
        ForecastHorizon activeHorizon = (horizon != null) ? horizon : ForecastHorizon.DAYS_30;

        Forecaster forecaster = forecasterMap.get(activeMethod);
        if (forecaster == null) {
            throw new BadRequestException("Unsupported forecasting method: " + activeMethod);
        }

        // Pull historical daily aggregated spend for the last 90 days to train the model
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(90);

        List<DailyCostSummary> summaries = billingRecordRepository.aggregateDailyCosts(
                org.getId(),
                activeCurrency,
                startDate,
                endDate);

        List<HistoricalDataPoint> historicalPoints = new ArrayList<>();
        if (summaries != null) {
            for (DailyCostSummary summary : summaries) {
                if (summary.getUsageDate() != null) {
                    BigDecimal cost = (summary.getTotalCost() != null) ? summary.getTotalCost() : BigDecimal.ZERO;
                    historicalPoints.add(HistoricalDataPoint.builder()
                            .date(summary.getUsageDate())
                            .cost(cost)
                            .build());
                }
            }
        }

        historicalPoints.sort(Comparator.comparing(HistoricalDataPoint::getDate));

        log.debug("Generating {} forecast for Org ID [{}] over horizon [{}] with {} historical data points",
                activeMethod, org.getId(), activeHorizon, historicalPoints.size());

        return forecaster.forecast(historicalPoints, activeHorizon, activeCurrency, alpha, windowSize);
    }

    private String resolveAndValidateCurrency(Long orgId, String requestedCurrency) {
        List<String> currencies = billingRecordRepository.findDistinctCurrenciesByOrganizationId(orgId);
        if (currencies == null || currencies.isEmpty()) {
            return (requestedCurrency != null && !requestedCurrency.trim().isEmpty())
                    ? requestedCurrency.trim().toUpperCase()
                    : "USD";
        }

        if (requestedCurrency != null && !requestedCurrency.trim().isEmpty()) {
            String clean = requestedCurrency.trim().toUpperCase();
            if (!currencies.contains(clean)) {
                log.warn("Requested currency [{}] not found in organization billing records: {}", clean, currencies);
            }
            return clean;
        }

        if (currencies.size() > 1) {
            log.warn("Multiple distinct billing currencies detected for organization [{}]: {}", orgId, currencies);
            throw new BadRequestException(
                    "Multiple distinct billing currencies detected in organization records: " + currencies +
                    ". Please specify a currency parameter to generate forecast.");
        }

        return currencies.get(0);
    }
}

