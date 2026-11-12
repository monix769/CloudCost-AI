package com.cloudcostai.service;

import com.cloudcostai.dto.GeneratedReportResponse;
import com.cloudcostai.dto.ReportGenerateRequest;
import com.cloudcostai.entity.AuditActionType;
import com.cloudcostai.entity.GeneratedReport;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.entity.ReportFormat;
import com.cloudcostai.entity.ReportStatus;
import com.cloudcostai.entity.ReportType;
import com.cloudcostai.exception.BadRequestException;
import com.cloudcostai.exception.ResourceNotFoundException;
import com.cloudcostai.report.engine.ReportEngineService;
import com.cloudcostai.repository.GeneratedReportRepository;
import com.cloudcostai.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final GeneratedReportRepository reportRepository;
    private final ReportEngineService reportEngineService;
    private final OrganizationService organizationService;
    private final AuditLogService auditLogService;

    // Secure local reports storage directory
    private static final String REPORT_BASE_DIR = "reports_storage";

    @Transactional
    public GeneratedReportResponse generateReport(ReportGenerateRequest request) {
        Organization org = organizationService.getCurrentUserOrganization();
        Long orgId = org.getId();

        String userEmail = getCurrentUserEmail();
        Long userId = getCurrentUserId();

        String ext = request.getFileFormat() == ReportFormat.PDF ? "pdf" : "csv";
        String safeName = (request.getReportName() != null && !request.getReportName().trim().isEmpty())
                ? request.getReportName().trim().replaceAll("[^a-zA-Z0-9_-]", "_")
                : request.getReportType().name().toLowerCase() + "_report";

        String fileName = String.format("%s_%s_%d.%s", safeName, UUID.randomUUID().toString().substring(0, 8), System.currentTimeMillis(), ext);

        byte[] content;
        try {
            content = reportEngineService.generateReport(org, request);
        } catch (Exception e) {
            log.error("Failed to generate report content for org [{}]: {}", orgId, e.getMessage(), e);
            GeneratedReport failed = GeneratedReport.builder()
                    .organization(org)
                    .reportName(safeName)
                    .reportType(request.getReportType())
                    .fileFormat(request.getFileFormat())
                    .filePath("FAILED")
                    .fileSizeBytes(0L)
                    .generatedByUserId(userId)
                    .generatedByUserEmail(userEmail)
                    .generationStatus(ReportStatus.FAILED)
                    .errorMessage(e.getMessage())
                    .startDate(request.getStartDate())
                    .endDate(request.getEndDate())
                    .provider(request.getProvider())
                    .currency(request.getCurrency())
                    .build();
            return toResponse(reportRepository.save(failed));
        }

        // Store file securely
        Path storageDirPath = Paths.get(REPORT_BASE_DIR, "org_" + orgId);
        try {
            Files.createDirectories(storageDirPath);
            Path filePath = storageDirPath.resolve(fileName).normalize();

            // Strict path traversal security check
            if (!filePath.startsWith(storageDirPath.normalize())) {
                throw new BadRequestException("Invalid file destination path (potential path traversal detected)");
            }

            Files.write(filePath, content);

            GeneratedReport report = GeneratedReport.builder()
                    .organization(org)
                    .reportName(safeName)
                    .reportType(request.getReportType())
                    .fileFormat(request.getFileFormat())
                    .filePath(filePath.toString())
                    .fileSizeBytes((long) content.length)
                    .generatedByUserId(userId)
                    .generatedByUserEmail(userEmail)
                    .generationStatus(ReportStatus.COMPLETED)
                    .startDate(request.getStartDate())
                    .endDate(request.getEndDate())
                    .provider(request.getProvider())
                    .currency(request.getCurrency())
                    .downloadCount(0)
                    .build();

            GeneratedReport saved = reportRepository.save(report);

            auditLogService.logCurrent(
                    AuditActionType.REPORT_GENERATED,
                    "REPORT",
                    saved.getId().toString(),
                    String.format("Generated %s report '%s' (Format: %s, Size: %d bytes)",
                            saved.getReportType(), saved.getReportName(), saved.getFileFormat(), saved.getFileSizeBytes())
            );

            log.info("Successfully generated report [ID: {}] for org [{}]", saved.getId(), orgId);
            return toResponse(saved);
        } catch (IOException e) {
            log.error("Failed to save report file on disk: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to persist report file: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Page<GeneratedReportResponse> getReports(ReportType type, ReportFormat format, Pageable pageable) {
        Organization org = organizationService.getCurrentUserOrganization();
        Page<GeneratedReport> page = reportRepository.findFilteredReports(org.getId(), type, format, null, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public GeneratedReportResponse getReportById(Long id) {
        GeneratedReport report = findReportEntity(id);
        return toResponse(report);
    }

    @Transactional
    public DownloadableReport downloadReport(Long id) {
        GeneratedReport report = findReportEntity(id);

        if (report.getGenerationStatus() != ReportStatus.COMPLETED) {
            throw new BadRequestException("Report is not ready for download. Current status: " + report.getGenerationStatus());
        }

        Path path = Paths.get(report.getFilePath()).normalize();
        Path baseDir = Paths.get(REPORT_BASE_DIR).normalize();

        // Path traversal guard
        if (!path.startsWith(baseDir)) {
            throw new BadRequestException("Security exception: Report path outside allowed storage root");
        }

        File file = path.toFile();
        if (!file.exists() || !file.isFile()) {
            throw new ResourceNotFoundException("Report file on disk", "id", id);
        }

        try {
            byte[] bytes = Files.readAllBytes(path);

            // Increment download counter
            report.setDownloadCount(report.getDownloadCount() + 1);
            reportRepository.save(report);

            auditLogService.logCurrent(
                    AuditActionType.REPORT_DOWNLOADED,
                    "REPORT",
                    report.getId().toString(),
                    String.format("Downloaded report '%s' (Count: %d)", report.getReportName(), report.getDownloadCount())
            );

            String contentType = report.getFileFormat() == ReportFormat.PDF ? "application/pdf" : "text/csv; charset=UTF-8";
            String downloadName = report.getReportName() + (report.getFileFormat() == ReportFormat.PDF ? ".pdf" : ".csv");

            return new DownloadableReport(downloadName, contentType, bytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read report file for download: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteReport(Long id) {
        GeneratedReport report = findReportEntity(id);

        // Delete physical file
        try {
            Path path = Paths.get(report.getFilePath()).normalize();
            Files.deleteIfExists(path);
        } catch (Exception e) {
            log.warn("Could not delete physical file for report [ID: {}]: {}", id, e.getMessage());
        }

        reportRepository.delete(report);

        auditLogService.logCurrent(
                AuditActionType.REPORT_DELETED,
                "REPORT",
                id.toString(),
                String.format("Deleted report '%s'", report.getReportName())
        );

        log.info("Report [ID: {}] deleted successfully", id);
    }

    @Transactional(readOnly = true)
    public GeneratedReport findReportEntity(Long id) {
        Organization org = organizationService.getCurrentUserOrganization();
        return reportRepository.findByIdAndOrganizationId(id, org.getId())
                .orElseThrow(() -> new ResourceNotFoundException("GeneratedReport", "id", id));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getId();
        }
        return null;
    }

    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            return auth.getName();
        }
        return "SYSTEM";
    }

    private GeneratedReportResponse toResponse(GeneratedReport r) {
        if (r == null) return null;
        return GeneratedReportResponse.builder()
                .id(r.getId())
                .reportName(r.getReportName())
                .reportType(r.getReportType() != null ? r.getReportType().name() : null)
                .fileFormat(r.getFileFormat() != null ? r.getFileFormat().name() : null)
                .fileSizeBytes(r.getFileSizeBytes())
                .generatedByUserEmail(r.getGeneratedByUserEmail())
                .generationStatus(r.getGenerationStatus() != null ? r.getGenerationStatus().name() : null)
                .errorMessage(r.getErrorMessage())
                .startDate(r.getStartDate())
                .endDate(r.getEndDate())
                .provider(r.getProvider() != null ? r.getProvider().name() : null)
                .currency(r.getCurrency())
                .downloadCount(r.getDownloadCount())
                .createdAt(r.getCreatedAt())
                .build();
    }

    public record DownloadableReport(String fileName, String contentType, byte[] data) {}
}

