package com.cloudcostai.controller;

import com.cloudcostai.dto.ApiResponse;
import com.cloudcostai.dto.BudgetRequest;
import com.cloudcostai.dto.BudgetResponse;
import com.cloudcostai.service.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Budget Management", description = "Operations for configuring financial thresholds and monitoring live spend utilization")
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    @Operation(summary = "List all budgets", description = "Retrieve all organization budgets with live calculated spend utilization metrics")
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getBudgets() {
        log.debug("GET /api/budgets");
        List<BudgetResponse> budgets = budgetService.getAllBudgets();
        return ResponseEntity.ok(ApiResponse.success(budgets));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get budget by ID", description = "Retrieve a single budget configuration and its live utilization metrics")
    public ResponseEntity<ApiResponse<BudgetResponse>> getBudgetById(
            @Parameter(description = "Budget record ID") @PathVariable Long id) {
        log.debug("GET /api/budgets/{}", id);
        BudgetResponse budget = budgetService.getBudgetById(id);
        return ResponseEntity.ok(ApiResponse.success(budget));
    }

    @PostMapping
    @Operation(summary = "Create budget", description = "Define a new financial ceiling for cloud expenditure tracking")
    public ResponseEntity<ApiResponse<BudgetResponse>> createBudget(
            @Valid @RequestBody BudgetRequest request) {
        log.debug("POST /api/budgets - name: {}, amount: {}", request.getName(), request.getAmountLimit());
        BudgetResponse created = budgetService.createBudget(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Budget created successfully", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update budget", description = "Modify an existing budget ceiling, warning threshold, or period")
    public ResponseEntity<ApiResponse<BudgetResponse>> updateBudget(
            @Parameter(description = "Budget record ID") @PathVariable Long id,
            @Valid @RequestBody BudgetRequest request) {
        log.debug("PUT /api/budgets/{}", id);
        BudgetResponse updated = budgetService.updateBudget(id, request);
        return ResponseEntity.ok(ApiResponse.success("Budget updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete budget", description = "Remove an existing budget configuration")
    public ResponseEntity<ApiResponse<Void>> deleteBudget(
            @Parameter(description = "Budget record ID") @PathVariable Long id) {
        log.debug("DELETE /api/budgets/{}", id);
        budgetService.deleteBudget(id);
        return ResponseEntity.ok(ApiResponse.success("Budget deleted successfully", null));
    }
}
