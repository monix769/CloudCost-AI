package com.cloudcostai.controller;

import com.cloudcostai.dto.ApiResponse;
import com.cloudcostai.dto.BillingRecordRequest;
import com.cloudcostai.dto.BillingRecordResponse;
import com.cloudcostai.dto.BillingUploadResponse;
import com.cloudcostai.entity.CloudProvider;
import com.cloudcostai.service.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Billing Management", description = "Endpoints for line-item billing record management and CSV bulk ingestion")
public class BillingController {

    private final BillingService billingService;

    @GetMapping
    @Operation(summary = "Get paginated billing records", description = "Query billing records with filtering by account, provider, service and date range")
    public ResponseEntity<ApiResponse<Page<BillingRecordResponse>>> getBillingRecords(
            @Parameter(description = "Associated Cloud Account ID")
            @RequestParam(required = false) Long cloudAccountId,
            @Parameter(description = "Filter by Cloud Provider (AWS, AZURE, GCP)")
            @RequestParam(required = false) CloudProvider provider,
            @Parameter(description = "Filter by partial Service Name")
            @RequestParam(required = false) String serviceName,
            @Parameter(description = "Filter start date (inclusive, YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Filter end date (inclusive, YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort direction (asc/desc)")
            @RequestParam(defaultValue = "desc") String direction,
            @Parameter(description = "Sort property field")
            @RequestParam(defaultValue = "billingPeriodStart") String sortBy) {

        Sort sort = "asc".equalsIgnoreCase(direction) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, size), sort);

        Page<BillingRecordResponse> records = billingService.getBillingRecords(
                cloudAccountId, provider, serviceName, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success(records));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get billing record by ID", description = "Retrieve a single line-item billing entry")
    public ResponseEntity<ApiResponse<BillingRecordResponse>> getBillingRecordById(
            @Parameter(description = "Billing Record ID") @PathVariable Long id) {
        log.debug("GET /api/billing/{}", id);
        BillingRecordResponse record = billingService.getBillingRecordById(id);
        return ResponseEntity.ok(ApiResponse.success(record));
    }

    @PostMapping
    @Operation(summary = "Create individual billing record", description = "Manually record a new billing item")
    public ResponseEntity<ApiResponse<BillingRecordResponse>> createBillingRecord(
            @Valid @RequestBody BillingRecordRequest request) {
        log.debug("POST /api/billing - service: {}, cost: {}", request.getServiceName(), request.getCostAmount());
        BillingRecordResponse record = billingService.createBillingRecord(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Billing record created successfully", record));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload CSV billing data", description = "Bulk ingest billing records via standard CSV file. Validates headers and rows, skipping duplicates.")
    public ResponseEntity<ApiResponse<BillingUploadResponse>> uploadBillingCsv(
            @Parameter(description = "CSV billing file to upload", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
            @RequestParam("file") MultipartFile file) {
        log.info("POST /api/billing/upload - processing file: {}", file.getOriginalFilename());
        BillingUploadResponse result = billingService.processCsvUpload(file);
        return ResponseEntity.ok(ApiResponse.success("CSV billing upload processed", result));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete billing record", description = "Remove an existing line item from billing records")
    public ResponseEntity<ApiResponse<Void>> deleteBillingRecord(
            @Parameter(description = "Billing Record ID") @PathVariable Long id) {
        log.debug("DELETE /api/billing/{}", id);
        billingService.deleteBillingRecord(id);
        return ResponseEntity.ok(ApiResponse.success("Billing record deleted successfully", null));
    }
}
