package com.cloudcostai.report.engine;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class CsvReportBuilder {

    public byte[] buildCsv(ReportDataModel data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(out, StandardCharsets.UTF_8), CSVFormat.DEFAULT)) {
            // Report Metadata Header
            printer.printRecord("Report", data.getReportTitle());
            printer.printRecord("Organization", data.getOrganizationName());
            printer.printRecord("Period Start", data.getStartDate() != null ? data.getStartDate().toString() : "All-Time");
            printer.printRecord("Period End", data.getEndDate() != null ? data.getEndDate().toString() : "Present");
            printer.printRecord("Currency", data.getCurrency());
            printer.printRecord("Generated At", data.getGeneratedAt().toString());
            printer.println();

            // Table Headers
            if (data.getTableHeaders() != null && !data.getTableHeaders().isEmpty()) {
                printer.printRecord(data.getTableHeaders());
            }

            // Table Rows
            if (data.getTableRows() != null) {
                for (List<String> row : data.getTableRows()) {
                    printer.printRecord(row);
                }
            }

            printer.flush();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CSV report: " + e.getMessage(), e);
        }
    }
}
