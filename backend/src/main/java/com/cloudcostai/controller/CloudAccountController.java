package com.cloudcostai.controller;

import com.cloudcostai.dto.ApiResponse;
import com.cloudcostai.dto.CloudAccountRequest;
import com.cloudcostai.dto.CloudAccountResponse;
import com.cloudcostai.entity.CloudProvider;
import com.cloudcostai.service.CloudAccountService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cloud-accounts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cloud Accounts", description = "Operations for connecting and managing AWS, Azure, and GCP accounts")
public class CloudAccountController {

    private final CloudAccountService cloudAccountService;
    private final com.cloudcostai.cloud.service.CloudProviderService cloudProviderService;

    @GetMapping
    @Operation(summary = "List cloud accounts", description = "Retrieve all registered cloud provider accounts, optionally filtered by provider")
    public ResponseEntity<ApiResponse<List<CloudAccountResponse>>> getCloudAccounts(
            @Parameter(description = "Filter by cloud provider (AWS, AZURE, GCP)")
            @RequestParam(required = false) CloudProvider provider) {
        log.debug("GET /api/cloud-accounts - provider filter: {}", provider);
        List<CloudAccountResponse> accounts = cloudAccountService.getAllAccounts(provider);
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get cloud account by ID", description = "Retrieve a single cloud account record by its primary key ID")
    public ResponseEntity<ApiResponse<CloudAccountResponse>> getCloudAccountById(
            @Parameter(description = "Cloud Account ID") @PathVariable Long id) {
        log.debug("GET /api/cloud-accounts/{}", id);
        CloudAccountResponse account = cloudAccountService.getAccountById(id);
        return ResponseEntity.ok(ApiResponse.success(account));
    }

    @PostMapping
    @Operation(summary = "Create cloud account", description = "Register a new cloud provider account for cost monitoring")
    public ResponseEntity<ApiResponse<CloudAccountResponse>> createCloudAccount(
            @Valid @RequestBody CloudAccountRequest request) {
        log.debug("POST /api/cloud-accounts - provider: {}, accountId: {}", request.getProvider(), request.getAccountId());
        CloudAccountResponse created = cloudAccountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Cloud account registered successfully", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update cloud account", description = "Update configuration and metadata for an existing cloud account")
    public ResponseEntity<ApiResponse<CloudAccountResponse>> updateCloudAccount(
            @Parameter(description = "Cloud Account ID") @PathVariable Long id,
            @Valid @RequestBody CloudAccountRequest request) {
        log.debug("PUT /api/cloud-accounts/{}", id);
        CloudAccountResponse updated = cloudAccountService.updateAccount(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cloud account updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete cloud account", description = "Remove a cloud account connection and related references")
    public ResponseEntity<ApiResponse<Void>> deleteCloudAccount(
            @Parameter(description = "Cloud Account ID") @PathVariable Long id) {
        log.debug("DELETE /api/cloud-accounts/{}", id);
        cloudAccountService.deleteAccount(id);
        return ResponseEntity.ok(ApiResponse.success("Cloud account deleted successfully", null));
    }

    @PostMapping("/{id}/connect")
    @Operation(summary = "Connect cloud credentials", description = "Validate and securely store encrypted credentials for an existing cloud account")
    public ResponseEntity<ApiResponse<CloudAccountResponse>> connectCloudAccount(
            @Parameter(description = "Cloud Account ID") @PathVariable Long id,
            @RequestBody com.cloudcostai.cloud.provider.ProviderCredentials credentials) {
        log.info("POST /api/cloud-accounts/{}/connect", id);
        CloudAccountResponse connected = cloudProviderService.connectAccount(id, credentials);
        return ResponseEntity.ok(ApiResponse.success("Cloud account connected successfully", connected));
    }

    @PostMapping("/{id}/validate")
    @Operation(summary = "Validate cloud connection", description = "Test connectivity and verify IAM permissions for a connected account")
    public ResponseEntity<ApiResponse<com.cloudcostai.cloud.provider.ConnectionValidationResult>> validateCloudAccount(
            @Parameter(description = "Cloud Account ID") @PathVariable Long id) {
        log.info("POST /api/cloud-accounts/{}/validate", id);
        com.cloudcostai.cloud.provider.ConnectionValidationResult result = cloudProviderService.validateAccountConnection(id);
        return ResponseEntity.ok(ApiResponse.success("Connection validation completed", result));
    }

    @PostMapping("/{id}/disconnect")
    @Operation(summary = "Disconnect cloud account", description = "Purge encrypted credentials and revert connection state")
    public ResponseEntity<ApiResponse<CloudAccountResponse>> disconnectCloudAccount(
            @Parameter(description = "Cloud Account ID") @PathVariable Long id) {
        log.info("POST /api/cloud-accounts/{}/disconnect", id);
        CloudAccountResponse disconnected = cloudProviderService.disconnectAccount(id);
        return ResponseEntity.ok(ApiResponse.success("Cloud account disconnected", disconnected));
    }

    @GetMapping("/{id}/connection-status")
    @Operation(summary = "Get cloud connection status", description = "Inspect current connection, sync health, and credential status")
    public ResponseEntity<ApiResponse<com.cloudcostai.dto.ConnectionStatusResponse>> getConnectionStatus(
            @Parameter(description = "Cloud Account ID") @PathVariable Long id) {
        log.debug("GET /api/cloud-accounts/{}/connection-status", id);
        com.cloudcostai.dto.ConnectionStatusResponse status = cloudProviderService.getConnectionStatus(id);
        return ResponseEntity.ok(ApiResponse.success(status));
    }
}
