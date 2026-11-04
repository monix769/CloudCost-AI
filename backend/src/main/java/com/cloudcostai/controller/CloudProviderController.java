package com.cloudcostai.controller;

import com.cloudcostai.cloud.provider.ConnectionValidationResult;
import com.cloudcostai.cloud.provider.ProviderCapabilities;
import com.cloudcostai.cloud.provider.ProviderCredentials;
import com.cloudcostai.cloud.service.CloudProviderService;
import com.cloudcostai.dto.ApiResponse;
import com.cloudcostai.entity.CloudProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cloud-providers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cloud Providers", description = "Capabilities and metadata for supported cloud platforms (AWS, Azure, GCP)")
public class CloudProviderController {

    private final CloudProviderService cloudProviderService;

    @GetMapping
    @Operation(summary = "List supported cloud providers", description = "Retrieve capabilities, supported dimensions, and prerequisites for all supported cloud providers")
    public ResponseEntity<ApiResponse<List<ProviderCapabilities>>> getSupportedProviders() {
        log.debug("GET /api/cloud-providers");
        List<ProviderCapabilities> capabilities = cloudProviderService.getAllCapabilities();
        return ResponseEntity.ok(ApiResponse.success(capabilities));
    }

    @GetMapping("/{provider}/capabilities")
    @Operation(summary = "Get provider capabilities", description = "Retrieve detailed capabilities, reporting delays, and IAM requirements for a specific provider")
    public ResponseEntity<ApiResponse<ProviderCapabilities>> getProviderCapabilities(
            @Parameter(description = "Cloud Provider (AWS, AZURE, GCP)") @PathVariable CloudProvider provider) {
        log.debug("GET /api/cloud-providers/{}/capabilities", provider);
        ProviderCapabilities capabilities = cloudProviderService.getCapabilities(provider);
        return ResponseEntity.ok(ApiResponse.success(capabilities));
    }

    @PostMapping("/{provider}/test-connection")
    @Operation(summary = "Test provider credentials ad-hoc", description = "Validate candidate credentials without persisting them or modifying existing account state")
    public ResponseEntity<ApiResponse<ConnectionValidationResult>> testConnection(
            @Parameter(description = "Cloud Provider (AWS, AZURE, GCP)") @PathVariable CloudProvider provider,
            @RequestBody ProviderCredentials credentials) {
        log.info("POST /api/cloud-providers/{}/test-connection", provider);
        ConnectionValidationResult result = cloudProviderService.testCredentials(provider, credentials);
        return ResponseEntity.ok(ApiResponse.success(result.isValid() ? "Credentials validated successfully" : "Credential validation failed", result));
    }
}
