package com.cloudcostai.cloud.provider.aws;

import com.cloudcostai.cloud.provider.CloudBillingProvider;
import com.cloudcostai.cloud.provider.ConnectionValidationResult;
import com.cloudcostai.cloud.provider.NormalizedBillingRecord;
import com.cloudcostai.cloud.provider.ProviderCapabilities;
import com.cloudcostai.cloud.provider.ProviderCredentials;
import com.cloudcostai.entity.BillingSourceType;
import com.cloudcostai.entity.CloudAccount;
import com.cloudcostai.entity.CloudProvider;
import com.cloudcostai.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.costexplorer.CostExplorerClient;
import software.amazon.awssdk.services.costexplorer.model.CostExplorerException;
import software.amazon.awssdk.services.costexplorer.model.DateInterval;
import software.amazon.awssdk.services.costexplorer.model.GetCostAndUsageRequest;
import software.amazon.awssdk.services.costexplorer.model.GetCostAndUsageResponse;
import software.amazon.awssdk.services.costexplorer.model.GetDimensionValuesRequest;
import software.amazon.awssdk.services.costexplorer.model.Granularity;
import software.amazon.awssdk.services.costexplorer.model.Group;
import software.amazon.awssdk.services.costexplorer.model.GroupDefinition;
import software.amazon.awssdk.services.costexplorer.model.GroupDefinitionType;
import software.amazon.awssdk.services.costexplorer.model.MetricValue;
import software.amazon.awssdk.services.costexplorer.model.ResultByTime;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class AwsBillingProvider implements CloudBillingProvider {

    @Override
    public CloudProvider getProvider() {
        return CloudProvider.AWS;
    }

    @Override
    public ConnectionValidationResult validateConnection(CloudAccount account, ProviderCredentials credentials) {
        if (credentials == null) {
            return ConnectionValidationResult.builder()
                    .valid(false)
                    .message("AWS credentials are missing. Please provide Access Key ID and Secret Access Key.")
                    .missingPermissions(List.of("ce:GetCostAndUsage", "ce:GetDimensionValues"))
                    .build();
        }

        // Check if mock adapter explicitly requested for testing/sandboxing
        if (Boolean.TRUE.equals(credentials.getUseMockAdapter()) || isMockKey(credentials.getAwsAccessKeyId())) {
            log.info("AWS provider validating via mock test adapter for account: {}", account.getAccountId());
            return ConnectionValidationResult.builder()
                    .valid(true)
                    .mockValidation(true)
                    .message("AWS test connection validated successfully (Simulated Test Adapter).")
                    .validatedPermissions(List.of("ce:GetCostAndUsage", "ce:GetDimensionValues"))
                    .providerAccountId(account.getAccountId())
                    .validatedAt(Instant.now())
                    .build();
        }

        if (credentials.getAwsAccessKeyId() == null || credentials.getAwsSecretAccessKey() == null) {
            return ConnectionValidationResult.builder()
                    .valid(false)
                    .message("AWS Access Key ID and Secret Access Key are required.")
                    .missingPermissions(List.of("ce:GetCostAndUsage", "ce:GetDimensionValues"))
                    .build();
        }

        try (CostExplorerClient ceClient = buildClient(credentials)) {
            // Probe Cost Explorer dimension values to verify live API authorization
            LocalDate now = LocalDate.now();
            DateInterval interval = DateInterval.builder()
                    .start(now.minusDays(2).toString())
                    .end(now.toString())
                    .build();

            GetDimensionValuesRequest probeRequest = GetDimensionValuesRequest.builder()
                    .timePeriod(interval)
                    .dimension("SERVICE")
                    .build();

            ceClient.getDimensionValues(probeRequest);

            return ConnectionValidationResult.builder()
                    .valid(true)
                    .message("AWS Cost Explorer connection validated successfully.")
                    .validatedPermissions(List.of("ce:GetCostAndUsage", "ce:GetDimensionValues"))
                    .providerAccountId(account.getAccountId())
                    .validatedAt(Instant.now())
                    .build();
        } catch (CostExplorerException e) {
            log.warn("AWS Cost Explorer validation failed: {}", e.awsErrorDetails().errorMessage());
            return ConnectionValidationResult.builder()
                    .valid(false)
                    .message("AWS Cost Explorer error: " + e.awsErrorDetails().errorMessage())
                    .missingPermissions(List.of("ce:GetCostAndUsage"))
                    .build();
        } catch (Exception e) {
            log.error("AWS validation failed with unexpected error: {}", e.getMessage());
            return ConnectionValidationResult.builder()
                    .valid(false)
                    .message("Failed to connect to AWS: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public List<NormalizedBillingRecord> fetchBillingData(
            CloudAccount account,
            ProviderCredentials credentials,
            LocalDate startDate,
            LocalDate endDate) {

        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date cannot be after end date");
        }

        if (credentials != null && (Boolean.TRUE.equals(credentials.getUseMockAdapter()) || isMockKey(credentials.getAwsAccessKeyId()))) {
            return generateMockAwsRecords(account, startDate, endDate);
        }

        if (credentials == null || credentials.getAwsAccessKeyId() == null) {
            throw new BadRequestException("AWS credentials not configured for cloud account: " + account.getId());
        }

        List<NormalizedBillingRecord> records = new ArrayList<>();

        try (CostExplorerClient ceClient = buildClient(credentials)) {
            // AWS Cost Explorer end date is exclusive, so add 1 day
            DateInterval interval = DateInterval.builder()
                    .start(startDate.toString())
                    .end(endDate.plusDays(1).toString())
                    .build();

            GetCostAndUsageRequest request = GetCostAndUsageRequest.builder()
                    .timePeriod(interval)
                    .granularity(Granularity.DAILY)
                    .metrics("UnblendedCost")
                    .groupBy(GroupDefinition.builder()
                            .type(GroupDefinitionType.DIMENSION)
                            .key("SERVICE")
                            .build())
                    .build();

            GetCostAndUsageResponse response = ceClient.getCostAndUsage(request);

            for (ResultByTime resultByTime : response.resultsByTime()) {
                LocalDate periodStart = LocalDate.parse(resultByTime.timePeriod().start());
                LocalDate periodEnd = LocalDate.parse(resultByTime.timePeriod().end()).minusDays(1);

                for (Group group : resultByTime.groups()) {
                    String serviceName = group.keys().isEmpty() ? "Unknown AWS Service" : group.keys().get(0);
                    MetricValue unblendedCost = group.metrics().get("UnblendedCost");

                    if (unblendedCost != null && unblendedCost.amount() != null) {
                        BigDecimal amount = new BigDecimal(unblendedCost.amount());
                        if (amount.compareTo(BigDecimal.ZERO) > 0) {
                            String currency = unblendedCost.unit() != null ? unblendedCost.unit() : "USD";

                            records.add(NormalizedBillingRecord.builder()
                                    .provider(CloudProvider.AWS)
                                    .serviceName(serviceName)
                                    .region(credentials.getAwsRegion() != null ? credentials.getAwsRegion() : "us-east-1")
                                    .costAmount(amount)
                                    .currency(currency)
                                    .usageType("BoxUsage")
                                    .billingPeriodStart(periodStart)
                                    .billingPeriodEnd(periodEnd)
                                    .sourceType(BillingSourceType.REAL_PROVIDER)
                                    .build());
                        }
                    }
                }
            }
        } catch (CostExplorerException e) {
            log.error("AWS Cost Explorer query failed: {}", e.awsErrorDetails().errorMessage());
            throw new RuntimeException("AWS Cost Explorer query failed: " + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            log.error("Failed to query AWS billing data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to query AWS billing data: " + e.getMessage(), e);
        }

        return records;
    }

    @Override
    public ProviderCapabilities getCapabilities() {
        return ProviderCapabilities.builder()
                .provider(CloudProvider.AWS)
                .providerName("Amazon Web Services")
                .supportedApi("AWS Cost Explorer API (GetCostAndUsage)")
                .reportingLag("24 to 48 hours (standard AWS reporting delay)")
                .requiredPermissions(List.of("ce:GetCostAndUsage", "ce:GetDimensionValues"))
                .supportedDimensions(List.of("SERVICE", "REGION", "USAGE_TYPE", "LINKED_ACCOUNT"))
                .liveSupported(true)
                .documentationUrl("https://docs.aws.amazon.com/cost-management/latest/userguide/ce-api.html")
                .authType("IAM Access Key & Secret or IAM Role Assumption")
                .build();
    }

    private CostExplorerClient buildClient(ProviderCredentials credentials) {
        String regionStr = credentials.getAwsRegion() != null ? credentials.getAwsRegion() : "us-east-1";
        return CostExplorerClient.builder()
                .region(Region.of(regionStr))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(credentials.getAwsAccessKeyId(), credentials.getAwsSecretAccessKey())))
                .build();
    }

    private boolean isMockKey(String key) {
        return key != null && (key.startsWith("MOCK-") || key.startsWith("TEST-") || key.equals("mock-aws-key"));
    }

    private List<NormalizedBillingRecord> generateMockAwsRecords(CloudAccount account, LocalDate start, LocalDate end) {
        List<NormalizedBillingRecord> mockRecords = new ArrayList<>();
        String[] services = {"Amazon Elastic Compute Cloud - Compute", "Amazon Simple Storage Service", "Amazon Relational Database Service", "Amazon DynamoDB"};
        BigDecimal[] costs = {new BigDecimal("42.8500"), new BigDecimal("12.3000"), new BigDecimal("35.7500"), new BigDecimal("8.2000")};

        for (int i = 0; i < services.length; i++) {
            mockRecords.add(NormalizedBillingRecord.builder()
                    .provider(CloudProvider.AWS)
                    .providerRecordId("aws-mock-" + account.getAccountId() + "-" + (i + 1))
                    .serviceName(services[i])
                    .region("us-east-1")
                    .costAmount(costs[i])
                    .currency("USD")
                    .usageType("VirtualMachines")
                    .usageQuantity(new BigDecimal("720.0000"))
                    .usageUnit("Hours")
                    .billingPeriodStart(start)
                    .billingPeriodEnd(end)
                    .sourceType(BillingSourceType.MOCK_PROVIDER)
                    .build());
        }
        return mockRecords;
    }
}
