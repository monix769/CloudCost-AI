package com.cloudcostai.cloud.provider;

import com.cloudcostai.entity.CloudAccount;
import com.cloudcostai.entity.CloudProvider;

import java.time.LocalDate;
import java.util.List;

/**
 * Common SPI contract implemented by all cloud billing provider adapters
 * (AWS Cost Explorer, Azure Cost Management, GCP BigQuery Billing Export).
 */
public interface CloudBillingProvider {

    /**
     * Identifies the cloud provider handled by this adapter.
     */
    CloudProvider getProvider();

    /**
     * Validates connection credentials and tests whether required FinOps/billing permissions
     * are granted on the target cloud account.
     * Note: Requirement 6 - Do NOT claim a provider is connected unless credentials and permissions succeed.
     */
    ConnectionValidationResult validateConnection(CloudAccount account, ProviderCredentials credentials);

    /**
     * Queries cloud billing APIs for itemized expenditure over the requested date range.
     * Emits standardized NormalizedBillingRecord line items.
     */
    List<NormalizedBillingRecord> fetchBillingData(
            CloudAccount account,
            ProviderCredentials credentials,
            LocalDate startDate,
            LocalDate endDate);

    /**
     * Metadata describing provider capabilities, reporting delays, and IAM prerequisites.
     */
    ProviderCapabilities getCapabilities();
}
