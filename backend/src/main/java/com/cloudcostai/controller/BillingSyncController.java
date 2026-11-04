package com.cloudcostai.controller;

import com.cloudcostai.cloud.service.BillingIngestionService;
import com.cloudcostai.dto.ApiResponse;
import com.cloudcostai.dto.SyncHistoryResponse;
import com.cloudcostai.entity.SyncHistory;
import com.cloudcostai.mapper.SyncHistoryMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/billing/sync")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Billing Synchronization", description = "Endpoints for triggering multi-cloud billing synchronization and auditing sync history")
public class BillingSyncController {

    private final BillingIngestionService billingIngestionService;
    private final SyncHistoryMapper syncHistoryMapper;

    @PostMapping("/{cloudAccountId}")
    @Operation(summary = "Trigger cloud billing synchronization", description = "Manually initiate billing data retrieval from the configured cloud provider API")
    public ResponseEntity<ApiResponse<SyncHistoryResponse>> triggerSync(
            @Parameter(description = "Cloud Account ID") @PathVariable Long cloudAccountId,
            @Parameter(description = "Query window start date (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Query window end date (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("POST /api/billing/sync/{} [Range: {} to {}]", cloudAccountId, startDate, endDate);
        SyncHistory history = billingIngestionService.synchronizeAccount(cloudAccountId, startDate, endDate, true);
        return ResponseEntity.ok(ApiResponse.success("Cloud billing synchronization completed", syncHistoryMapper.toResponse(history)));
    }

    @GetMapping("/history")
    @Operation(summary = "Get synchronization history", description = "Retrieve audit log of billing sync executions, optionally filtered by cloud account")
    public ResponseEntity<ApiResponse<Page<SyncHistoryResponse>>> getSyncHistory(
            @Parameter(description = "Filter by cloud account ID")
            @RequestParam(required = false) Long cloudAccountId,
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort direction (asc/desc)")
            @RequestParam(defaultValue = "desc") String direction,
            @Parameter(description = "Sort property")
            @RequestParam(defaultValue = "syncStartTime") String sortBy) {

        Sort sort = "asc".equalsIgnoreCase(direction) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, size), sort);

        Page<SyncHistoryResponse> historyPage = billingIngestionService.getSyncHistory(cloudAccountId, pageable)
                .map(syncHistoryMapper::toResponse);

        return ResponseEntity.ok(ApiResponse.success(historyPage));
    }

    @GetMapping("/history/{syncId}")
    @Operation(summary = "Get sync history details by ID", description = "Inspect detailed results and error summaries for a specific sync execution")
    public ResponseEntity<ApiResponse<SyncHistoryResponse>> getSyncHistoryById(
            @Parameter(description = "Sync History ID") @PathVariable Long syncId) {
        log.debug("GET /api/billing/sync/history/{}", syncId);
        SyncHistory history = billingIngestionService.getSyncHistoryById(syncId);
        return ResponseEntity.ok(ApiResponse.success(syncHistoryMapper.toResponse(history)));
    }

    @GetMapping("/status/{cloudAccountId}")
    @Operation(summary = "Get latest sync status for account", description = "Check whether an account has succeeded, failed, or has a sync currently in progress")
    public ResponseEntity<ApiResponse<SyncHistoryResponse>> getLatestSyncStatus(
            @Parameter(description = "Cloud Account ID") @PathVariable Long cloudAccountId) {
        log.debug("GET /api/billing/sync/status/{}", cloudAccountId);
        return billingIngestionService.getLatestSyncStatus(cloudAccountId)
                .map(h -> ResponseEntity.ok(ApiResponse.success(syncHistoryMapper.toResponse(h))))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.success("No sync history found for account", null)));
    }
}
