package com.cloudcostai.report.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PdfReportBuilderTest {

    private final PdfReportBuilder builder = new PdfReportBuilder();

    @Test
    @DisplayName("Should generate valid PDF byte stream with %PDF- header")
    void shouldGenerateValidPdfStream() {
        ReportDataModel model = ReportDataModel.builder()
                .reportTitle("Executive FinOps Summary")
                .organizationName("Acme Global Corp")
                .startDate(LocalDate.now().minusDays(30))
                .endDate(LocalDate.now())
                .currency("USD")
                .generatedAt(LocalDate.now())
                .executiveSummary("Comprehensive spending overview across AWS, Azure, and Google Cloud workloads.")
                .totalSpend(new BigDecimal("12450.75"))
                .anomalyCount(3)
                .potentialMonthlySavings(new BigDecimal("1850.00"))
                .realizedMonthlySavings(new BigDecimal("420.00"))
                .tableHeaders(Arrays.asList("Provider", "Monthly Cost ($)", "Workload Share (%)"))
                .tableRows(Arrays.asList(
                        Arrays.asList("AWS", "7450.50", "59.8%"),
                        Arrays.asList("AZURE", "3500.25", "28.1%"),
                        Arrays.asList("GCP", "1500.00", "12.1%")
                ))
                .insights(Arrays.asList(
                        "Identified 12 rightsizing candidates in AWS us-east-1.",
                        "Locking in 1-Year Savings Plan yields estimated $850/mo reduction."
                ))
                .build();

        byte[] pdfBytes = builder.buildPdf(model);

        assertThat(pdfBytes).isNotEmpty();
        assertThat(pdfBytes.length).isGreaterThan(1000);

        // Verify valid PDF magic bytes header: %PDF-
        String header = new String(pdfBytes, 0, Math.min(pdfBytes.length, 5));
        assertThat(header).isEqualTo("%PDF-");
    }
}

