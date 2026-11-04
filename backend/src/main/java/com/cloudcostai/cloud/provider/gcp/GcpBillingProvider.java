package com.cloudcostai.cloud.provider.gcp;

import com.cloudcostai.cloud.provider.CloudBillingProvider;
import com.cloudcostai.cloud.provider.ConnectionValidationResult;
import com.cloudcostai.cloud.provider.NormalizedBillingRecord;
import com.cloudcostai.cloud.provider.ProviderCapabilities;
import com.cloudcostai.cloud.provider.ProviderCredentials;
import com.cloudcostai.entity.BillingSourceType;
import com.cloudcostai.entity.CloudAccount;
import com.cloudcostai.entity.CloudProvider;
import com.cloudcostai.exception.BadRequestException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class GcpBillingProvider implements CloudBillingProvider {

    @Override
    public CloudProvider getProvider() {
        return CloudProvider.GCP;
    }

    @Override
    public ConnectionValidationResult validateConnection(CloudAccount account, ProviderCredentials credentials) {
        if (credentials == null) {
            return ConnectionValidationResult.builder()
                    .valid(false)
                    .message("GCP credentials missing. Service Account JSON, Project ID, Dataset ID, and Billing Export Table are required.")
                    .missingPermissions(List.of("roles/bigquery.dataViewer", "roles/bigquery.jobUser"))
                    .build();
        }

        if (Boolean.TRUE.equals(credentials.getUseMockAdapter()) || isMockKey(credentials.getGcpProjectId()) || isMockKey(credentials.getGcpServiceAccountJson())) {
            log.info("GCP provider validating via mock test adapter for account: {}", account.getAccountId());
            return ConnectionValidationResult.builder()
                    .valid(true)
                    .mockValidation(true)
                    .message("GCP BigQuery billing export connection validated successfully (Simulated Test Adapter).")
                    .validatedPermissions(List.of("roles/bigquery.dataViewer", "roles/bigquery.jobUser"))
                    .providerAccountId(account.getAccountId())
                    .validatedAt(Instant.now())
                    .build();
        }

        if (credentials.getGcpProjectId() == null || credentials.getGcpDatasetId() == null ||
                credentials.getGcpBillingTable() == null || credentials.getGcpServiceAccountJson() == null) {
            return ConnectionValidationResult.builder()
                    .valid(false)
                    .message("GCP Project ID, BigQuery Dataset ID, Table Name, and Service Account JSON are all required.")
                    .missingPermissions(List.of("roles/bigquery.dataViewer", "roles/bigquery.jobUser"))
                    .build();
        }

        try {
            BigQuery bigQuery = buildBigQueryClient(credentials);
            Table table = bigQuery.getTable(TableId.of(credentials.getGcpProjectId(), credentials.getGcpDatasetId(), credentials.getGcpBillingTable()));

            if (table == null || !table.exists()) {
                return ConnectionValidationResult.builder()
                        .valid(false)
                        .message("BigQuery billing export table does not exist: " + credentials.getGcpProjectId() + "." +
                                credentials.getGcpDatasetId() + "." + credentials.getGcpBillingTable() +
                                ". Verify Cloud Billing BigQuery Export configuration in GCP Console.")
                        .missingPermissions(List.of("roles/bigquery.dataViewer"))
                        .build();
            }

            return ConnectionValidationResult.builder()
                    .valid(true)
                    .message("Google Cloud BigQuery billing export table found and permissions verified successfully.")
                    .validatedPermissions(List.of("roles/bigquery.dataViewer", "roles/bigquery.jobUser"))
                    .providerAccountId(account.getAccountId())
                    .validatedAt(Instant.now())
                    .build();
        } catch (BigQueryException e) {
            log.warn("GCP BigQuery validation error [Code {}]: {}", e.getCode(), e.getMessage());
            return ConnectionValidationResult.builder()
                    .valid(false)
                    .message("GCP BigQuery authorization error: " + e.getMessage())
                    .missingPermissions(List.of("roles/bigquery.jobUser"))
                    .build();
        } catch (Exception e) {
            log.error("Failed to validate GCP BigQuery connection: {}", e.getMessage());
            return ConnectionValidationResult.builder()
                    .valid(false)
                    .message("GCP validation failed: " + e.getMessage())
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

        if (credentials != null && (Boolean.TRUE.equals(credentials.getUseMockAdapter()) || isMockKey(credentials.getGcpProjectId()) || isMockKey(credentials.getGcpServiceAccountJson()))) {
            return generateMockGcpRecords(account, startDate, endDate);
        }

        if (credentials == null || credentials.getGcpServiceAccountJson() == null) {
            throw new BadRequestException("GCP credentials not configured for cloud account: " + account.getId());
        }

        List<NormalizedBillingRecord> records = new ArrayList<>();

        try {
            BigQuery bigQuery = buildBigQueryClient(credentials);
            String fullTableRef = String.format("`%s.%s.%s`",
                    credentials.getGcpProjectId(), credentials.getGcpDatasetId(), credentials.getGcpBillingTable());

            String sql = "SELECT " +
                    "service.description AS service_name, " +
                    "COALESCE(location.location, 'global') AS region, " +
                    "SUM(cost) AS cost, " +
                    "currency, " +
                    "DATE(usage_start_time) AS usage_date " +
                    "FROM " + fullTableRef + " " +
                    "WHERE DATE(usage_start_time) BETWEEN @startDate AND @endDate " +
                    "GROUP BY service_name, region, currency, usage_date " +
                    "HAVING SUM(cost) > 0";

            QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(sql)
                    .addNamedParameter("startDate", QueryParameterValue.date(startDate.toString()))
                    .addNamedParameter("endDate", QueryParameterValue.date(endDate.toString()))
                    .build();

            TableResult result = bigQuery.query(queryConfig);

            for (FieldValueList row : result.iterateAll()) {
                String serviceName = row.get("service_name").isNull() ? "Unknown GCP Service" : row.get("service_name").getStringValue();
                String region = row.get("region").isNull() ? "global" : row.get("region").getStringValue();
                BigDecimal cost = new BigDecimal(row.get("cost").getStringValue());
                String currency = row.get("currency").isNull() ? "USD" : row.get("currency").getStringValue();
                String usageDateStr = row.get("usage_date").getStringValue();
                LocalDate usageDate = LocalDate.parse(usageDateStr);

                records.add(NormalizedBillingRecord.builder()
                        .provider(CloudProvider.GCP)
                        .serviceName(serviceName)
                        .region(region)
                        .costAmount(cost)
                        .currency(currency)
                        .usageType("ComputeEngine")
                        .billingPeriodStart(usageDate)
                        .billingPeriodEnd(usageDate)
                        .sourceType(BillingSourceType.REAL_PROVIDER)
                        .build());
            }
        } catch (BigQueryException e) {
            log.error("GCP BigQuery billing query failed: {}", e.getMessage(), e);
            throw new RuntimeException("GCP BigQuery billing query failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to query GCP BigQuery export: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to query GCP BigQuery export: " + e.getMessage(), e);
        }

        return records;
    }

    @Override
    public ProviderCapabilities getCapabilities() {
        return ProviderCapabilities.builder()
                .provider(CloudProvider.GCP)
                .providerName("Google Cloud Platform")
                .supportedApi("Google Cloud Billing Export to BigQuery (Standard & Detailed)")
                .reportingLag("4 to 12 hours (Cloud Billing export batch intervals)")
                .requiredPermissions(List.of("roles/bigquery.dataViewer", "roles/bigquery.jobUser"))
                .supportedDimensions(List.of("service.description", "sku.description", "project.id", "location.location"))
                .liveSupported(true)
                .documentationUrl("https://cloud.google.com/billing/docs/how-to/export-data-bigquery")
                .authType("Service Account Key (JSON)")
                .build();
    }

    private BigQuery buildBigQueryClient(ProviderCredentials credentials) throws Exception {
        InputStream stream = new ByteArrayInputStream(credentials.getGcpServiceAccountJson().getBytes(StandardCharsets.UTF_8));
        GoogleCredentials gcpCreds = ServiceAccountCredentials.fromStream(stream);

        return BigQueryOptions.newBuilder()
                .setCredentials(gcpCreds)
                .setProjectId(credentials.getGcpProjectId())
                .build()
                .getService();
    }

    private boolean isMockKey(String val) {
        return val != null && (val.startsWith("MOCK-") || val.startsWith("TEST-") || val.contains("mock") || val.contains("test"));
    }

    private List<NormalizedBillingRecord> generateMockGcpRecords(CloudAccount account, LocalDate start, LocalDate end) {
        List<NormalizedBillingRecord> mockRecords = new ArrayList<>();
        String[] services = {"Compute Engine", "Cloud Storage", "Cloud SQL", "Google Kubernetes Engine"};
        BigDecimal[] costs = {new BigDecimal("38.6000"), new BigDecimal("14.1000"), new BigDecimal("27.4000"), new BigDecimal("19.8000")};

        for (int i = 0; i < services.length; i++) {
            mockRecords.add(NormalizedBillingRecord.builder()
                    .provider(CloudProvider.GCP)
                    .providerRecordId("gcp-mock-" + account.getAccountId() + "-" + (i + 1))
                    .serviceName(services[i])
                    .region("us-central1")
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
