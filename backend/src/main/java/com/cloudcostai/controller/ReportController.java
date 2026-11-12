package com.cloudcostai.controller;

import com.cloudcostai.dto.ApiResponse;
import com.cloudcostai.dto.GeneratedReportResponse;
import com.cloudcostai.dto.ReportGenerateRequest;
import com.cloudcostai.entity.ReportFormat;
import com.cloudcostai.entity.ReportType;
import com.cloudcostai.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reporting Engine", description = "Endpoints for generating, inspecting, downloading, and deleting professional FinOps PDF & CSV reports")
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    @Operation(summary = "List generated reports", description = "Retrieve a paginated list of previously generated reports for the current organization")
    public ResponseEntity<ApiResponse<Page<GeneratedReportResponse>>> getReports(
            @Parameter(description = "Filter by report type")
            @RequestParam(required = false) ReportType type,
            @Parameter(description = "Filter by format (PDF or CSV)")
            @RequestParam(required = false) ReportFormat format,
            @Parameter(description = "Page number (0-indexed, default 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default 20, max 100)")
            @RequestParam(defaultValue = "20") int size) {

        log.debug("GET /api/reports - type: {}, format: {}, page: {}, size: {}", type, format, page, size);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<GeneratedReportResponse> paged = reportService.getReports(type, format, pageable);
        return ResponseEntity.ok(ApiResponse.success(paged));
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate a new report", description = "Create an executive FinOps PDF or CSV report based on historical billing and analytics data")
    public ResponseEntity<ApiResponse<GeneratedReportResponse>> generateReport(
            @Valid @RequestBody ReportGenerateRequest request) {
        log.info("POST /api/reports/generate - type: {}, format: {}", request.getReportType(), request.getFileFormat());
        GeneratedReportResponse report = reportService.generateReport(request);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get report details", description = "Retrieve metadata and download statistics for a specific report")
    public ResponseEntity<ApiResponse<GeneratedReportResponse>> getReportById(
            @Parameter(description = "Report ID") @PathVariable Long id) {
        log.debug("GET /api/reports/{}", id);
        GeneratedReportResponse report = reportService.getReportById(id);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download report file", description = "Stream the generated PDF or CSV document with authentication and path traversal protection")
    public ResponseEntity<byte[]> downloadReport(
            @Parameter(description = "Report ID") @PathVariable Long id) {
        log.info("GET /api/reports/{}/download", id);
        ReportService.DownloadableReport file = reportService.downloadReport(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.fileName() + "\"")
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.data());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete generated report", description = "Remove the report record and associated file from storage (Administrator only)")
    public ResponseEntity<ApiResponse<String>> deleteReport(
            @Parameter(description = "Report ID") @PathVariable Long id) {
        log.info("DELETE /api/reports/{}", id);
        reportService.deleteReport(id);
        return ResponseEntity.ok(ApiResponse.success("Report deleted successfully"));
    }
}

