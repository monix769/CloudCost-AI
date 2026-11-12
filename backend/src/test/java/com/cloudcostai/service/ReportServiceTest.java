package com.cloudcostai.service;

import com.cloudcostai.dto.GeneratedReportResponse;
import com.cloudcostai.dto.ReportGenerateRequest;
import com.cloudcostai.entity.GeneratedReport;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.entity.ReportFormat;
import com.cloudcostai.entity.ReportStatus;
import com.cloudcostai.entity.ReportType;
import com.cloudcostai.exception.BadRequestException;
import com.cloudcostai.report.engine.ReportEngineService;
import com.cloudcostai.repository.GeneratedReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private GeneratedReportRepository reportRepository;

    @Mock
    private ReportEngineService reportEngineService;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ReportService reportService;

    private Organization testOrg;

    @BeforeEach
    void setUp() {
        testOrg = Organization.builder().name("Test Org").build();
        testOrg.setId(1L);
    }

    @Test
    @DisplayName("Should generate report and save record with correct metadata")
    void shouldGenerateReportSuccessfully() {
        when(organizationService.getCurrentUserOrganization()).thenReturn(testOrg);
        when(reportEngineService.generateReport(eq(testOrg), any(ReportGenerateRequest.class)))
                .thenReturn("sample-pdf-bytes".getBytes());

        when(reportRepository.save(any(GeneratedReport.class))).thenAnswer(inv -> {
            GeneratedReport r = inv.getArgument(0);
            r.setId(50L);
            return r;
        });

        ReportGenerateRequest req = ReportGenerateRequest.builder()
                .reportName("Quarterly FinOps Review")
                .reportType(ReportType.EXECUTIVE_SUMMARY)
                .fileFormat(ReportFormat.PDF)
                .startDate(LocalDate.now().minusDays(90))
                .endDate(LocalDate.now())
                .currency("USD")
                .build();

        GeneratedReportResponse resp = reportService.generateReport(req);

        assertThat(resp).isNotNull();
        assertThat(resp.getReportName()).contains("Quarterly_FinOps_Review");
        assertThat(resp.getFileFormat()).isEqualTo("PDF");
        assertThat(resp.getGenerationStatus()).isEqualTo("COMPLETED");
        assertThat(resp.getFileSizeBytes()).isGreaterThan(0L);
    }

    @Test
    @DisplayName("Should increment download count when report is downloaded")
    void shouldIncrementDownloadCount() throws IOException {
        when(organizationService.getCurrentUserOrganization()).thenReturn(testOrg);

        // Create temporary dummy file in reports_storage
        Path tempDir = Path.of("reports_storage", "org_1");
        Files.createDirectories(tempDir);
        Path tempFile = tempDir.resolve("test_download.pdf");
        Files.write(tempFile, "PDF DATA".getBytes());

        GeneratedReport report = GeneratedReport.builder()
                .organization(testOrg)
                .reportName("test_download")
                .reportType(ReportType.EXECUTIVE_SUMMARY)
                .fileFormat(ReportFormat.PDF)
                .filePath(tempFile.toString())
                .fileSizeBytes(8L)
                .generatedByUserEmail("admin@test.com")
                .generationStatus(ReportStatus.COMPLETED)
                .downloadCount(0)
                .build();
        report.setId(10L);

        when(reportRepository.findByIdAndOrganizationId(eq(10L), eq(1L))).thenReturn(Optional.of(report));
        when(reportRepository.save(any(GeneratedReport.class))).thenAnswer(inv -> inv.getArgument(0));

        ReportService.DownloadableReport downloaded = reportService.downloadReport(10L);

        assertThat(downloaded).isNotNull();
        assertThat(downloaded.fileName()).isEqualTo("test_download.pdf");
        assertThat(downloaded.contentType()).isEqualTo("application/pdf");
        assertThat(report.getDownloadCount()).isEqualTo(1);

        // Cleanup temp file
        Files.deleteIfExists(tempFile);
    }

    @Test
    @DisplayName("Should reject report download with path traversal attempt")
    void shouldRejectPathTraversal() {
        when(organizationService.getCurrentUserOrganization()).thenReturn(testOrg);

        GeneratedReport maliciousReport = GeneratedReport.builder()
                .organization(testOrg)
                .reportName("malicious_report")
                .reportType(ReportType.EXECUTIVE_SUMMARY)
                .fileFormat(ReportFormat.PDF)
                .filePath("reports_storage/../../etc/passwd") // Traversal
                .fileSizeBytes(100L)
                .generationStatus(ReportStatus.COMPLETED)
                .build();
        maliciousReport.setId(99L);

        when(reportRepository.findByIdAndOrganizationId(eq(99L), eq(1L))).thenReturn(Optional.of(maliciousReport));

        assertThatThrownBy(() -> reportService.downloadReport(99L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("outside allowed storage root");
    }
}

