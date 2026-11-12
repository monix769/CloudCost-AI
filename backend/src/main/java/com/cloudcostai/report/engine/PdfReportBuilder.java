package com.cloudcostai.report.engine;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.HeaderFooter;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
public class PdfReportBuilder {

    private static final Color PRIMARY_NAVY = new Color(15, 23, 42); // #0f172a
    private static final Color BRAND_BLUE = new Color(37, 99, 235);  // #2563eb
    private static final Color ACCENT_EMERALD = new Color(16, 185, 129); // #10b981
    private static final Color BG_LIGHT = new Color(248, 250, 252);   // #f8fafc
    private static final Color BORDER_GRAY = new Color(226, 232, 240); // #e2e8f0

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.WHITE);
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(203, 213, 225));
    private static final Font SECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, BRAND_BLUE);
    private static final Font TH_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
    private static final Font BODY_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
    private static final Font BODY_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
    private static final Font FOOTER_FONT = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);

    public byte[] buildPdf(ReportDataModel data) {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);

            // Configure footer
            HeaderFooter footer = new HeaderFooter(
                    new Phrase("CloudCost AI FinOps Platform — Confidential | Page ", FOOTER_FONT),
                    new Phrase(" | Generated: " + data.getGeneratedAt().format(DateTimeFormatter.ISO_LOCAL_DATE), FOOTER_FONT)
            );
            footer.setBorder(Rectangle.NO_BORDER);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.setFooter(footer);

            document.open();

            // 1. Header Banner
            PdfPTable headerTable = new PdfPTable(1);
            headerTable.setWidthPercentage(100);

            PdfPCell bannerCell = new PdfPCell();
            bannerCell.setBackgroundColor(PRIMARY_NAVY);
            bannerCell.setPadding(16);
            bannerCell.setBorder(Rectangle.NO_BORDER);

            Paragraph title = new Paragraph("CloudCost AI — " + data.getReportTitle(), TITLE_FONT);
            title.setSpacingAfter(4);
            bannerCell.addElement(title);

            Paragraph orgDetails = new Paragraph(
                    String.format("Organization: %s | Period: %s to %s | Currency: %s",
                            data.getOrganizationName(),
                            data.getStartDate() != null ? data.getStartDate().toString() : "All-Time",
                            data.getEndDate() != null ? data.getEndDate().toString() : "Present",
                            data.getCurrency()),
                    SUBTITLE_FONT
            );
            bannerCell.addElement(orgDetails);
            headerTable.addCell(bannerCell);
            document.add(headerTable);

            document.add(new Paragraph(" "));

            // 2. Executive Summary
            Paragraph execHeader = new Paragraph("1. Executive Summary", SECTION_FONT);
            execHeader.setSpacingAfter(6);
            document.add(execHeader);

            Paragraph execBody = new Paragraph(data.getExecutiveSummary(), BODY_FONT);
            execBody.setSpacingAfter(12);
            document.add(execBody);

            // 3. Key Financial Metrics Table
            Paragraph kpiHeader = new Paragraph("2. Key Financial Indicators", SECTION_FONT);
            kpiHeader.setSpacingAfter(6);
            document.add(kpiHeader);

            PdfPTable kpiTable = new PdfPTable(4);
            kpiTable.setWidthPercentage(100);
            kpiTable.setSpacingAfter(14);

            addKpiCell(kpiTable, "Total Expenditure", formatCurrency(data.getTotalSpend(), data.getCurrency()), BRAND_BLUE);
            addKpiCell(kpiTable, "Active Anomalies", String.valueOf(data.getAnomalyCount()), new Color(225, 29, 72));
            addKpiCell(kpiTable, "Potential Monthly Savings", formatCurrency(data.getPotentialMonthlySavings(), data.getCurrency()), ACCENT_EMERALD);
            addKpiCell(kpiTable, "Realized Monthly Savings", formatCurrency(data.getRealizedMonthlySavings(), data.getCurrency()), new Color(13, 148, 136));

            document.add(kpiTable);

            // 4. Data Breakdown Table
            Paragraph dataHeader = new Paragraph("3. Detailed Cost Breakdown", SECTION_FONT);
            dataHeader.setSpacingAfter(6);
            document.add(dataHeader);

            if (data.getTableHeaders() != null && !data.getTableHeaders().isEmpty()) {
                PdfPTable dataTable = new PdfPTable(data.getTableHeaders().size());
                dataTable.setWidthPercentage(100);
                dataTable.setSpacingAfter(14);

                // Table Header
                for (String col : data.getTableHeaders()) {
                    PdfPCell th = new PdfPCell(new Phrase(col, TH_FONT));
                    th.setBackgroundColor(BRAND_BLUE);
                    th.setPadding(6);
                    th.setBorderColor(BORDER_GRAY);
                    dataTable.addCell(th);
                }

                // Table Rows
                boolean alt = false;
                for (List<String> row : data.getTableRows()) {
                    Color rowBg = alt ? BG_LIGHT : Color.WHITE;
                    alt = !alt;
                    for (String cellVal : row) {
                        PdfPCell td = new PdfPCell(new Phrase(cellVal != null ? cellVal : "-", BODY_FONT));
                        td.setBackgroundColor(rowBg);
                        td.setPadding(5);
                        td.setBorderColor(BORDER_GRAY);
                        dataTable.addCell(td);
                    }
                }
                document.add(dataTable);
            }

            // 5. Strategic Recommendations & Insights
            if (data.getInsights() != null && !data.getInsights().isEmpty()) {
                Paragraph insightsHeader = new Paragraph("4. FinOps Strategic Insights & Corrective Actions", SECTION_FONT);
                insightsHeader.setSpacingAfter(6);
                document.add(insightsHeader);

                for (String insight : data.getInsights()) {
                    Paragraph item = new Paragraph("• " + insight, BODY_FONT);
                    item.setSpacingAfter(4);
                    document.add(item);
                }
            }

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF report: " + e.getMessage(), e);
        }
    }

    private void addKpiCell(PdfPTable table, String title, String value, Color accent) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(BG_LIGHT);
        cell.setBorderColor(BORDER_GRAY);
        cell.setPadding(8);

        Paragraph pTitle = new Paragraph(title, BODY_FONT);
        Paragraph pVal = new Paragraph(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, accent));
        cell.addElement(pTitle);
        cell.addElement(pVal);
        table.addCell(cell);
    }

    private String formatCurrency(BigDecimal amount, String currency) {
        if (amount == null) return "$0.00";
        return String.format("$%s %s", amount.setScale(2, RoundingMode.HALF_UP).toPlainString(), currency != null ? currency : "USD");
    }
}

