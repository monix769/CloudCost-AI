package com.cloudcostai.report.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class CsvReportBuilderTest {

    private final CsvReportBuilder builder = new CsvReportBuilder();

    @Test
    @DisplayName("Should generate RFC-4180 compliant CSV document with metadata and data rows")
    void shouldGenerateRfc4180Csv() {
        ReportDataModel model = ReportDataModel.builder()
                .reportTitle("Provider Cost Comparison")
                .organizationName("Acme Global Corp")
                .startDate(LocalDate.now().minusDays(30))
                .endDate(LocalDate.now())
                .currency("USD")
                .generatedAt(LocalDate.now())
                .tableHeaders(Arrays.asList("Provider", "Cost", "Notes"))
                .tableRows(Arrays.asList(
                        Arrays.asList("AWS", "5000.00", "Production, Primary"),
                        Arrays.asList("AZURE", "2500.50", "Disaster Recovery \"Hot\" Standby")
                ))
                .build();

        byte[] csvBytes = builder.buildCsv(model);

        assertThat(csvBytes).isNotEmpty();
        String csvContent = new String(csvBytes, StandardCharsets.UTF_8);

        assertThat(csvContent).contains("Report,Provider Cost Comparison");
        assertThat(csvContent).contains("Organization,Acme Global Corp");
        assertThat(csvContent).contains("Provider,Cost,Notes");
        assertThat(csvContent).contains("\"Production, Primary\""); // RFC-4180 comma quoting
        assertThat(csvContent).contains("\"Disaster Recovery \"\"Hot\"\" Standby\""); // RFC-4180 escaped quote
    }
}
