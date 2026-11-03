package com.cloudcostai.controller;

import com.cloudcostai.dto.ApiResponse;
import com.cloudcostai.dto.StatusResponse;
import com.cloudcostai.service.StatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * System status controller providing detailed architectural and environment diagnostics.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "System Status", description = "Architectural metadata and runtime environment endpoints")
public class StatusController {

    private final StatusService statusService;

    @Autowired
    public StatusController(StatusService statusService) {
        this.statusService = statusService;
    }

    @GetMapping("/status")
    @Operation(summary = "System Architecture Status", description = "Returns active profiles, runtime versions, memory statistics, and enabled FinOps modules")
    public ResponseEntity<ApiResponse<StatusResponse>> getStatus() {
        StatusResponse status = statusService.getSystemStatus();
        return ResponseEntity.ok(ApiResponse.success("Status retrieved successfully", status));
    }
}
