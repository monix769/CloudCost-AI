package com.cloudcostai.service;

import com.cloudcostai.analytics.forecast.CostForecastService;
import com.cloudcostai.analytics.forecast.ForecastDataPoint;
import com.cloudcostai.analytics.forecast.ForecastHorizon;
import com.cloudcostai.analytics.forecast.ForecastMethod;
import com.cloudcostai.analytics.forecast.ForecastResult;
import com.cloudcostai.analytics.forecast.HistoricalDataPoint;
import com.cloudcostai.analytics.insight.FinOpsInsight;
import com.cloudcostai.analytics.insight.FinOpsInsightService;
import com.cloudcostai.dto.ProviderCostResponse;
import com.cloudcostai.dto.ServiceCostResponse;
import com.cloudcostai.dto.SpendingTrendPointResponse;
import com.cloudcostai.dto.SpendingTrendsSeriesResponse;
import com.cloudcostai.dto.TimeGranularity;
import com.cloudcostai.entity.CloudProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsExportService {

    private final CostAnalyticsService costAnalyticsService;
    private final CostForecastService costForecastService;
    private final FinOpsInsightService finOpsInsightService;

    public byte[] exportSpendingTrendsCsv(
            TimeGranularity granularity,
            LocalDate startDate,
            LocalDate endDate,
            String currency,
            CloudProvider provider) {

        SpendingTrendsSeriesResponse series = costAnalyticsService.getSpendingTrends(granularity, startDate, endDate, currency, provider);
        StringWriter writer = new StringWriter();

        try (CSVPrinter csv = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                .setHeader("Period", "Start Date", "End Date", "Currency", "Total Cost", "Avg Daily Cost",
                        "Min Cost", "Max Cost", "Percentage Change (%)", "AWS Cost", "Azure Cost", "GCP Cost", "Record Count")
                .build())) {

            for (SpendingTrendPointResponse pt : series.getPoints()) {
                csv.printRecord(
                        pt.getPeriod(),
                        pt.getStartDate() != null ? pt.getStartDate() : "",
                        pt.getEndDate() != null ? pt.getEndDate() : "",
                        series.getCurrency(),
                        pt.getTotalCost(),
                        pt.getAverageDailyCost(),
                        pt.getMinCost(),
                        pt.getMaxCost(),
                        pt.getPercentageChange() != null ? pt.getPercentageChange() : "",
                        pt.getAwsCost(),
                        pt.getAzureCost(),
                        pt.getGcpCost(),
                        pt.getRecordCount()
                );
            }
        } catch (Exception e) {
            log.error("Failed to generate spending trends CSV", e);
            throw new RuntimeException("CSV export generation failed", e);
        }

        return writer.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public byte[] exportProviderComparisonCsv(LocalDate startDate, LocalDate endDate, String currency) {
        List<ProviderCostResponse> providers = costAnalyticsService.getCostsByProvider(startDate, endDate, currency);
        StringWriter writer = new StringWriter();

        try (CSVPrinter csv = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                .setHeader("Provider", "Total Cost", "Currency", "Percentage Share (%)", "Record Count")
                .build())) {

            String cur = (currency != null && !currency.trim().isEmpty()) ? currency.toUpperCase() : "USD";
            for (ProviderCostResponse p : providers) {
                csv.printRecord(
                        p.getProvider().name(),
                        p.getTotalCost(),
                        cur,
                        p.getPercentage(),
                        p.getRecordCount()
                );
            }
        } catch (Exception e) {
            log.error("Failed to generate provider comparison CSV", e);
            throw new RuntimeException("CSV export generation failed", e);
        }

        return writer.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public byte[] exportServiceBreakdownCsv(LocalDate startDate, LocalDate endDate, Integer limit, String currency) {
        List<ServiceCostResponse> services = costAnalyticsService.getCostsByService(startDate, endDate, limit, currency);
        StringWriter writer = new StringWriter();

        try (CSVPrinter csv = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                .setHeader("Service Name", "Provider", "Total Cost", "Currency", "Percentage Share (%)", "Record Count")
                .build())) {

            String cur = (currency != null && !currency.trim().isEmpty()) ? currency.toUpperCase() : "USD";
            for (ServiceCostResponse s : services) {
                csv.printRecord(
                        s.getServiceName(),
                        s.getProvider() != null ? s.getProvider().name() : "",
                        s.getTotalCost(),
                        cur,
                        s.getPercentage(),
                        s.getRecordCount()
                );
            }
        } catch (Exception e) {
            log.error("Failed to generate service breakdown CSV", e);
            throw new RuntimeException("CSV export generation failed", e);
        }

        return writer.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public byte[] exportForecastCsv(ForecastHorizon horizon, ForecastMethod method, Double alpha, Integer windowSize, String currency) {
        ForecastResult result = costForecastService.generateForecast(horizon, method, alpha, windowSize, currency);
        StringWriter writer = new StringWriter();

        try (CSVPrinter csv = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                .setHeader("Record Type", "Date", "Cost", "Lower Bound (95%)", "Upper Bound (95%)", "Currency", "Method", "Horizon")
                .build())) {

            for (HistoricalDataPoint h : result.getHistoricalData()) {
                csv.printRecord(
                        "HISTORICAL",
                        h.getDate(),
                        h.getCost(),
                        "",
                        "",
                        result.getCurrency(),
                        result.getMethod().name(),
                        result.getHorizon()
                );
            }

            for (ForecastDataPoint f : result.getForecastData()) {
                csv.printRecord(
                        "FORECAST",
                        f.getDate(),
                        f.getForecastedCost(),
                        f.getLowerBound(),
                        f.getUpperBound(),
                        result.getCurrency(),
                        result.getMethod().name(),
                        result.getHorizon()
                );
            }
        } catch (Exception e) {
            log.error("Failed to generate forecast CSV", e);
            throw new RuntimeException("CSV export generation failed", e);
        }

        return writer.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public byte[] exportInsightsCsv(String currency) {
        List<FinOpsInsight> insights = finOpsInsightService.generateInsights(currency);
        StringWriter writer = new StringWriter();

        try (CSVPrinter csv = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                .setHeader("Insight ID", "Severity", "Category", "Title", "Description",
                        "Related Provider", "Related Service", "Related Account", "Financial Impact", "Currency", "Recommendation", "Detected At")
                .build())) {

            for (FinOpsInsight ins : insights) {
                csv.printRecord(
                        ins.getId(),
                        ins.getSeverity().name(),
                        ins.getCategory() != null ? ins.getCategory() : "",
                        ins.getTitle(),
                        ins.getDescription(),
                        ins.getRelatedProvider() != null ? ins.getRelatedProvider().name() : "",
                        ins.getRelatedService() != null ? ins.getRelatedService() : "",
                        ins.getRelatedAccount() != null ? ins.getRelatedAccount() : "",
                        ins.getFinancialImpact() != null ? ins.getFinancialImpact() : "",
                        ins.getCurrency(),
                        ins.getRecommendation(),
                        ins.getDetectionTimestamp()
                );
            }
        } catch (Exception e) {
            log.error("Failed to generate insights CSV", e);
            throw new RuntimeException("CSV export generation failed", e);
        }

        return writer.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}

