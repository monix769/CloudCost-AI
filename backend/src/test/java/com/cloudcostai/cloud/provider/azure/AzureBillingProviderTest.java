package com.cloudcostai.cloud.provider.azure;

import com.cloudcostai.cloud.provider.ConnectionValidationResult;
import com.cloudcostai.cloud.provider.NormalizedBillingRecord;
import com.cloudcostai.cloud.provider.ProviderCapabilities;
import com.cloudcostai.cloud.provider.ProviderCredentials;
import com.cloudcostai.entity.BillingSourceType;
import com.cloudcostai.entity.CloudAccount;
import com.cloudcostai.entity.CloudProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AzureBillingProviderTest {

    private AzureBillingProvider azureBillingProvider;
    private CloudAccount testAccount;

    @BeforeEach
    void setUp() {
        azureBillingProvider = new AzureBillingProvider(new ObjectMapper());
        testAccount = CloudAccount.builder()
                .accountId("sub-azure-12345")
                .accountName("Test Azure Subscription")
                .provider(CloudProvider.AZURE)
                .build();
        testAccount.setId(2L);
    }

    @Test
    @DisplayName("Should return AZURE CloudProvider identifier")
    void shouldReturnAzureProvider() {
        assertThat(azureBillingProvider.getProvider()).isEqualTo(CloudProvider.AZURE);
    }

    @Test
    @DisplayName("Should validate connection via mock test adapter when mock secret is provided")
    void shouldValidateConnectionViaMockAdapter() {
        ProviderCredentials creds = ProviderCredentials.builder()
                .azureTenantId("tenant-uuid-123")
                .azureClientId("client-uuid-456")
                .azureClientSecret("MOCK-azure-client-secret")
                .azureSubscriptionId("sub-uuid-789")
                .useMockAdapter(true)
                .build();

        ConnectionValidationResult result = azureBillingProvider.validateConnection(testAccount, creds);

        assertThat(result.isValid()).isTrue();
        assertThat(result.isMockValidation()).isTrue();
        assertThat(result.getValidatedPermissions()).contains("Cost Management Reader");
    }

    @Test
    @DisplayName("Should reject validation when Azure credentials are incomplete")
    void shouldRejectWhenCredentialsIncomplete() {
        ConnectionValidationResult nullResult = azureBillingProvider.validateConnection(testAccount, null);
        assertThat(nullResult.isValid()).isFalse();

        ProviderCredentials incomplete = ProviderCredentials.builder()
                .azureTenantId("tenant-123")
                .build();
        ConnectionValidationResult result = azureBillingProvider.validateConnection(testAccount, incomplete);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessage()).contains("required");
    }

    @Test
    @DisplayName("Should return normalized records with MOCK_PROVIDER source type when mock adapter is active")
    void shouldReturnNormalizedRecordsWithMockSource() {
        ProviderCredentials creds = ProviderCredentials.builder()
                .azureClientSecret("MOCK-secret")
                .useMockAdapter(true)
                .build();

        LocalDate start = LocalDate.of(2026, 5, 1);
        LocalDate end = LocalDate.of(2026, 5, 31);

        List<NormalizedBillingRecord> records = azureBillingProvider.fetchBillingData(testAccount, creds, start, end);

        assertThat(records).isNotEmpty();
        for (NormalizedBillingRecord rec : records) {
            assertThat(rec.getProvider()).isEqualTo(CloudProvider.AZURE);
            assertThat(rec.getSourceType()).isEqualTo(BillingSourceType.MOCK_PROVIDER);
            assertThat(rec.getCostAmount()).isPositive();
        }
    }

    @Test
    @DisplayName("Should expose Azure Cost Management capabilities and OAuth documentation")
    void shouldReturnAzureCapabilities() {
        ProviderCapabilities capabilities = azureBillingProvider.getCapabilities();

        assertThat(capabilities.getProvider()).isEqualTo(CloudProvider.AZURE);
        assertThat(capabilities.getProviderName()).isEqualTo("Microsoft Azure");
        assertThat(capabilities.getSupportedApi()).contains("Azure Cost Management Query API");
        assertThat(capabilities.getReportingLag()).contains("8 to 24 hours");
        assertThat(capabilities.getRequiredPermissions()).contains("Cost Management Reader");
    }
}
