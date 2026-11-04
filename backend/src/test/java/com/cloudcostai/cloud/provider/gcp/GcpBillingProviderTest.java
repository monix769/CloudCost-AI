package com.cloudcostai.cloud.provider.gcp;

import com.cloudcostai.cloud.provider.ConnectionValidationResult;
import com.cloudcostai.cloud.provider.NormalizedBillingRecord;
import com.cloudcostai.cloud.provider.ProviderCapabilities;
import com.cloudcostai.cloud.provider.ProviderCredentials;
import com.cloudcostai.entity.BillingSourceType;
import com.cloudcostai.entity.CloudAccount;
import com.cloudcostai.entity.CloudProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GcpBillingProviderTest {

    private GcpBillingProvider gcpBillingProvider;
    private CloudAccount testAccount;

    @BeforeEach
    void setUp() {
        gcpBillingProvider = new GcpBillingProvider();
        testAccount = CloudAccount.builder()
                .accountId("gcp-project-123")
                .accountName("Test GCP Project")
                .provider(CloudProvider.GCP)
                .build();
        testAccount.setId(3L);
    }

    @Test
    @DisplayName("Should return GCP CloudProvider identifier")
    void shouldReturnGcpProvider() {
        assertThat(gcpBillingProvider.getProvider()).isEqualTo(CloudProvider.GCP);
    }

    @Test
    @DisplayName("Should validate connection via mock test adapter when mock key is provided")
    void shouldValidateConnectionViaMockAdapter() {
        ProviderCredentials creds = ProviderCredentials.builder()
                .gcpProjectId("MOCK-project-id")
                .gcpDatasetId("billing_dataset")
                .gcpBillingTable("gcp_billing_export_v1_000000")
                .gcpServiceAccountJson("{\"type\": \"service_account\", \"project_id\": \"mock-project\"}")
                .useMockAdapter(true)
                .build();

        ConnectionValidationResult result = gcpBillingProvider.validateConnection(testAccount, creds);

        assertThat(result.isValid()).isTrue();
        assertThat(result.isMockValidation()).isTrue();
        assertThat(result.getValidatedPermissions()).contains("roles/bigquery.dataViewer", "roles/bigquery.jobUser");
    }

    @Test
    @DisplayName("Should reject validation when BigQuery dataset or table configurations are missing")
    void shouldRejectWhenDatasetOrTableMissing() {
        ProviderCredentials incomplete = ProviderCredentials.builder()
                .gcpProjectId("my-gcp-project")
                .build();

        ConnectionValidationResult result = gcpBillingProvider.validateConnection(testAccount, incomplete);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessage()).contains("required");
    }

    @Test
    @DisplayName("Should return normalized records with MOCK_PROVIDER source type when mock adapter is active")
    void shouldReturnNormalizedRecordsWithMockSource() {
        ProviderCredentials creds = ProviderCredentials.builder()
                .gcpProjectId("MOCK-project")
                .useMockAdapter(true)
                .build();

        LocalDate start = LocalDate.of(2026, 5, 1);
        LocalDate end = LocalDate.of(2026, 5, 31);

        List<NormalizedBillingRecord> records = gcpBillingProvider.fetchBillingData(testAccount, creds, start, end);

        assertThat(records).isNotEmpty();
        for (NormalizedBillingRecord rec : records) {
            assertThat(rec.getProvider()).isEqualTo(CloudProvider.GCP);
            assertThat(rec.getSourceType()).isEqualTo(BillingSourceType.MOCK_PROVIDER);
            assertThat(rec.getCostAmount()).isPositive();
            assertThat(rec.getCurrency()).isEqualTo("USD");
        }
    }

    @Test
    @DisplayName("Should expose Google Cloud BigQuery Billing Export capabilities and setup guidance")
    void shouldReturnGcpCapabilities() {
        ProviderCapabilities capabilities = gcpBillingProvider.getCapabilities();

        assertThat(capabilities.getProvider()).isEqualTo(CloudProvider.GCP);
        assertThat(capabilities.getProviderName()).isEqualTo("Google Cloud Platform");
        assertThat(capabilities.getSupportedApi()).contains("BigQuery");
        assertThat(capabilities.getReportingLag()).contains("4 to 12 hours");
        assertThat(capabilities.getRequiredPermissions()).contains("roles/bigquery.dataViewer", "roles/bigquery.jobUser");
    }
}
