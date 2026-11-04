package com.cloudcostai.cloud.provider;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified credential carrier for cloud providers.
 * Sensitive fields are NEVER printed in toString() or exposed in logs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProviderCredentials {

    // --- AWS Fields ---
    @JsonAlias({"awsAccessKeyId", "accessKeyId"})
    private String awsAccessKeyId;

    @JsonAlias({"awsSecretAccessKey", "secretAccessKey"})
    private String awsSecretAccessKey;

    @JsonAlias({"awsRoleArn", "roleArn"})
    private String awsRoleArn;

    @JsonAlias({"awsExternalId", "externalId"})
    private String awsExternalId;

    @JsonAlias({"awsRegion", "region"})
    private String awsRegion;

    // --- Azure Fields ---
    @JsonAlias({"azureTenantId", "tenantId"})
    private String azureTenantId;

    @JsonAlias({"azureClientId", "clientId"})
    private String azureClientId;

    @JsonAlias({"azureClientSecret", "clientSecret"})
    private String azureClientSecret;

    @JsonAlias({"azureSubscriptionId", "subscriptionId"})
    private String azureSubscriptionId;

    @JsonAlias({"azureResourceGroup", "resourceGroup"})
    private String azureResourceGroup;

    // --- GCP Fields ---
    @JsonAlias({"gcpProjectId", "projectId"})
    private String gcpProjectId;

    @JsonAlias({"gcpDatasetId", "datasetId"})
    private String gcpDatasetId;

    @JsonAlias({"gcpBillingTable", "tableName", "tableId"})
    private String gcpBillingTable;

    @JsonAlias({"gcpServiceAccountJson", "serviceAccountJson"})
    private String gcpServiceAccountJson;

    // Flag for mock/sandbox testing mode in development environments
    @JsonAlias({"useMockAdapter", "useMock", "mock"})
    private Boolean useMockAdapter;

    @Override
    public String toString() {
        return "ProviderCredentials{" +
                "awsAccessKeyId=" + (awsAccessKeyId != null ? mask(awsAccessKeyId) : "null") +
                ", awsRoleArn=" + (awsRoleArn != null ? awsRoleArn : "null") +
                ", awsRegion=" + awsRegion +
                ", azureTenantId=" + (azureTenantId != null ? mask(azureTenantId) : "null") +
                ", azureClientId=" + (azureClientId != null ? mask(azureClientId) : "null") +
                ", azureSubscriptionId=" + (azureSubscriptionId != null ? mask(azureSubscriptionId) : "null") +
                ", gcpProjectId=" + gcpProjectId +
                ", gcpDatasetId=" + gcpDatasetId +
                ", gcpBillingTable=" + gcpBillingTable +
                ", useMockAdapter=" + useMockAdapter +
                '}';
    }

    private static String mask(String s) {
        if (s == null || s.length() <= 4) {
            return "****";
        }
        return s.substring(0, 3) + "****" + s.substring(s.length() - 2);
    }
}
