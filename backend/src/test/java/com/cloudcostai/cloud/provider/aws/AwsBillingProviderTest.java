package com.cloudcostai.cloud.provider.aws;

import com.cloudcostai.cloud.provider.ConnectionValidationResult;
import com.cloudcostai.cloud.provider.NormalizedBillingRecord;
import com.cloudcostai.cloud.provider.ProviderCapabilities;
import com.cloudcostai.cloud.provider.ProviderCredentials;
import com.cloudcostai.entity.BillingSourceType;
import com.cloudcostai.entity.CloudAccount;
import com.cloudcostai.entity.CloudProvider;
import com.cloudcostai.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AwsBillingProviderTest {

    private AwsBillingProvider awsBillingProvider;
    private CloudAccount testAccount;

    @BeforeEach
    void setUp() {
        awsBillingProvider = new AwsBillingProvider();
        testAccount = CloudAccount.builder()
                .accountId("123456789012")
                .accountName("Test AWS Account")
                .provider(CloudProvider.AWS)
                .build();
        testAccount.setId(1L);
    }

    @Test
    @DisplayName("Should return AWS CloudProvider identifier")
    void shouldReturnAwsProvider() {
        assertThat(awsBillingProvider.getProvider()).isEqualTo(CloudProvider.AWS);
    }

    @Test
    @DisplayName("Should validate connection via mock test adapter when mock key is provided")
    void shouldValidateConnectionViaMockAdapter() {
        ProviderCredentials creds = ProviderCredentials.builder()
                .awsAccessKeyId("MOCK-AKIAIOSFODNN7EXAMPLE")
                .awsSecretAccessKey("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
                .useMockAdapter(true)
                .build();

        ConnectionValidationResult result = awsBillingProvider.validateConnection(testAccount, creds);

        assertThat(result.isValid()).isTrue();
        assertThat(result.isMockValidation()).isTrue();
        assertThat(result.getValidatedPermissions()).contains("ce:GetCostAndUsage", "ce:GetDimensionValues");
    }

    @Test
    @DisplayName("Should fail validation when AWS credentials are null or empty")
    void shouldFailValidationWhenCredentialsMissing() {
        ConnectionValidationResult nullResult = awsBillingProvider.validateConnection(testAccount, null);
        assertThat(nullResult.isValid()).isFalse();
        assertThat(nullResult.getMessage()).contains("missing");

        ProviderCredentials emptyCreds = ProviderCredentials.builder().build();
        ConnectionValidationResult emptyResult = awsBillingProvider.validateConnection(testAccount, emptyCreds);
        assertThat(emptyResult.isValid()).isFalse();
        assertThat(emptyResult.getMessage()).contains("required");
    }

    @Test
    @DisplayName("Should generate mock records with MOCK_PROVIDER source type when mock adapter is enabled")
    void shouldGenerateMockRecordsWithMockSourceType() {
        ProviderCredentials creds = ProviderCredentials.builder()
                .awsAccessKeyId("MOCK-KEY")
                .useMockAdapter(true)
                .build();

        LocalDate start = LocalDate.of(2026, 5, 1);
        LocalDate end = LocalDate.of(2026, 5, 31);

        List<NormalizedBillingRecord> records = awsBillingProvider.fetchBillingData(testAccount, creds, start, end);

        assertThat(records).isNotEmpty();
        for (NormalizedBillingRecord rec : records) {
            assertThat(rec.getProvider()).isEqualTo(CloudProvider.AWS);
            assertThat(rec.getSourceType()).isEqualTo(BillingSourceType.MOCK_PROVIDER);
            assertThat(rec.getCostAmount()).isPositive();
            assertThat(rec.getCurrency()).isEqualTo("USD");
        }
    }

    @Test
    @DisplayName("Should throw BadRequestException if startDate is after endDate")
    void shouldThrowWhenDateRangeIsInvalid() {
        ProviderCredentials creds = ProviderCredentials.builder().useMockAdapter(true).build();
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 5, 1);

        assertThatThrownBy(() -> awsBillingProvider.fetchBillingData(testAccount, creds, start, end))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Start date cannot be after end date");
    }

    @Test
    @DisplayName("Should return complete provider capabilities metadata")
    void shouldReturnProviderCapabilities() {
        ProviderCapabilities capabilities = awsBillingProvider.getCapabilities();

        assertThat(capabilities.getProvider()).isEqualTo(CloudProvider.AWS);
        assertThat(capabilities.getProviderName()).isEqualTo("Amazon Web Services");
        assertThat(capabilities.getRequiredPermissions()).contains("ce:GetCostAndUsage");
        assertThat(capabilities.getReportingLag()).contains("24 to 48 hours");
        assertThat(capabilities.isLiveSupported()).isTrue();
    }
}
