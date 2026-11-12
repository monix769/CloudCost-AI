package com.cloudcostai.controller;

import com.cloudcostai.dto.ApiResponse;
import com.cloudcostai.dto.AuditLogResponse;
import com.cloudcostai.entity.AuditActionType;
import com.cloudcostai.service.AuditLogService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Enterprise Audit Logging", description = "Endpoints for inspecting append-only immutable security and operational audit trails (Administrator only)")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "List audit logs", description = "Retrieve a filterable, paginated audit trail of all administrative and financial actions")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAuditLogs(
            @Parameter(description = "Filter by action type (e.g. LOGIN, REPORT_GENERATED, MEMBER_INVITED)")
            @RequestParam(required = false) AuditActionType actionType,
            @Parameter(description = "Filter by actor user email")
            @RequestParam(required = false) String userEmail,
            @Parameter(description = "Filter by target resource type (e.g. REPORT, USER, BUDGET)")
            @RequestParam(required = false) String resourceType,
            @Parameter(description = "Start date filter (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date filter (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Page number (0-indexed, default 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default 20, max 100)")
            @RequestParam(defaultValue = "20") int size) {

        log.debug("GET /api/audit-logs - action: {}, user: {}, resource: {}, page: {}", actionType, userEmail, resourceType, page);

        Instant startInstant = startDate != null ? startDate.atStartOfDay().toInstant(ZoneOffset.UTC) : null;
        Instant endInstant = endDate != null ? endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC) : null;

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLogResponse> paged = auditLogService.getAuditLogs(actionType, userEmail, resourceType, startInstant, endInstant, pageable);

        return ResponseEntity.ok(ApiResponse.success(paged));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get audit log details", description = "Inspect complete payload and metadata for an individual audit event")
    public ResponseEntity<ApiResponse<AuditLogResponse>> getAuditLogById(
            @Parameter(description = "Audit Log ID") @PathVariable Long id) {
        log.debug("GET /api/audit-logs/{}", id);
        AuditLogResponse response = auditLogService.getAuditLogById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

