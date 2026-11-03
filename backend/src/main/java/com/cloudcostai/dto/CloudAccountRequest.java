package com.cloudcostai.dto;

import com.cloudcostai.entity.CloudProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload to create or update a cloud provider account")
public class CloudAccountRequest {

    @NotNull(message = "Cloud provider is required (AWS, AZURE, GCP)")
    @Schema(description = "Cloud provider", example = "AWS")
    private CloudProvider provider;

    @NotBlank(message = "Account ID is required")
    @Size(max = 100, message = "Account ID cannot exceed 100 characters")
    @Schema(description = "Unique provider account/subscription/project ID", example = "123456789012")
    private String accountId;

    @NotBlank(message = "Account name is required")
    @Size(max = 150, message = "Account name cannot exceed 150 characters")
    @Schema(description = "Human-readable label for account", example = "Production AWS Account")
    private String accountName;

    @Size(max = 255, message = "ARN or Client ID cannot exceed 255 characters")
    @Schema(description = "IAM Role ARN or Client ID", example = "arn:aws:iam::123456789012:role/CloudCostRole")
    private String arnOrClientId;

    @Schema(description = "Account operational status", example = "true")
    @Builder.Default
    private Boolean isActive = true;
}
