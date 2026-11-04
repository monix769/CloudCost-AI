package com.cloudcostai.cloud.provider.azure;

import com.cloudcostai.cloud.provider.CloudBillingProvider;
import com.cloudcostai.cloud.provider.ConnectionValidationResult;
import com.cloudcostai.cloud.provider.NormalizedBillingRecord;
import com.cloudcostai.cloud.provider.ProviderCapabilities;
import com.cloudcostai.cloud.provider.ProviderCredentials;
import com.cloudcostai.entity.BillingSourceType;
import com.cloudcostai.entity.CloudAccount;
import com.cloudcostai.entity.CloudProvider;
import com.cloudcostai.exception.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class AzureBillingProvider implements CloudBillingProvider {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public AzureBillingProvider(ObjectMapper objectMapper) {
        this.restClient = RestClient.builder().build();
        this.objectMapper = objectMapper;
    }

    @Override
    public CloudProvider getProvider() {
        return CloudProvider.AZURE;
    }

    @Override
    public ConnectionValidationResult validateConnection(CloudAccount account, ProviderCredentials credentials) {
        if (credentials == null) {
            return ConnectionValidationResult.builder()
                    .valid(false)
                    .message("Azure credentials missing. Please supply Tenant ID, Client ID, Client Secret, and Subscription ID.")
                    .missingPermissions(List.of("Microsoft.CostManagement/query/action", "Cost Management Reader"))
                    .build();
        }

        if (Boolean.TRUE.equals(credentials.getUseMockAdapter()) || isMockSecret(credentials.getAzureClientSecret())) {
            log.info("Azure provider validating via mock test adapter for account: {}", account.getAccountId());
            return ConnectionValidationResult.builder()
                    .valid(true)
                    .mockValidation(true)
                    .message("Azure subscription connection validated successfully (Simulated Test Adapter).")
                    .validatedPermissions(List.of("Cost Management Reader", "Microsoft.CostManagement/query/action"))
                    .providerAccountId(account.getAccountId())
                    .validatedAt(Instant.now())
                    .build();
        }

        if (credentials.getAzureTenantId() == null || credentials.getAzureClientId() == null ||
                credentials.getAzureClientSecret() == null || credentials.getAzureSubscriptionId() == null) {
            return ConnectionValidationResult.builder()
                    .valid(false)
                    .message("Tenant ID, Client ID, Client Secret, and Subscription ID are required for Azure Entra ID.")
                    .missingPermissions(List.of("Cost Management Reader"))
                    .build();
        }

        try {
            // 1. Acquire Entra ID OAuth 2.0 Access Token
            String token = acquireAccessToken(credentials);
            if (token == null || token.isEmpty()) {
                return ConnectionValidationResult.builder()
                        .valid(false)
                        .message("Failed to acquire OAuth 2.0 access token from Microsoft Entra ID.")
                        .build();
            }

            // 2. Probe Cost Management Query API with empty 1-day range
            LocalDate today = LocalDate.now();
            queryAzureCostManagement(credentials, token, today.minusDays(1), today);

            return ConnectionValidationResult.builder()
                    .valid(true)
                    .message("Azure Cost Management connection and Entra ID permissions validated successfully.")
                    .validatedPermissions(List.of("Cost Management Reader", "Microsoft.CostManagement/query/action"))
                    .providerAccountId(account.getAccountId())
                    .validatedAt(Instant.now())
                    .build();
        } catch (HttpClientErrorException.Forbidden e) {
            log.warn("Azure permission denied: {}", e.getResponseBodyAsString());
            return ConnectionValidationResult.builder()
                    .valid(false)
                    .message("Azure 403 Forbidden: Service Principal lacks 'Cost Management Reader' role on subscription " + credentials.getAzureSubscriptionId())
                    .missingPermissions(List.of("Cost Management Reader"))
                    .build();
        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("Azure authentication unauthorized: {}", e.getResponseBodyAsString());
            return ConnectionValidationResult.builder()
                    .valid(false)
                    .message("Azure 401 Unauthorized: Invalid Client Secret or Client ID for Microsoft Entra tenant.")
                    .build();
        } catch (Exception e) {
            log.error("Azure connection validation error: {}", e.getMessage());
            return ConnectionValidationResult.builder()
                    .valid(false)
                    .message("Azure connection failed: " + e.getMessage())
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

        if (credentials != null && (Boolean.TRUE.equals(credentials.getUseMockAdapter()) || isMockSecret(credentials.getAzureClientSecret()))) {
            return generateMockAzureRecords(account, startDate, endDate);
        }

        if (credentials == null || credentials.getAzureSubscriptionId() == null) {
            throw new BadRequestException("Azure subscription credentials not configured for cloud account: " + account.getId());
        }

        String accessToken = acquireAccessToken(credentials);
        JsonNode responseNode = queryAzureCostManagement(credentials, accessToken, startDate, endDate);

        List<NormalizedBillingRecord> records = new ArrayList<>();
        JsonNode properties = responseNode.path("properties");
        JsonNode columns = properties.path("columns");
        JsonNode rows = properties.path("rows");

        // Map column names to row indexes
        Map<String, Integer> colIndexMap = new HashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            String colName = columns.get(i).path("name").asText();
            colIndexMap.put(colName.toLowerCase(), i);
        }

        int costIdx = colIndexMap.getOrDefault("cost", 0);
        int usageDateIdx = colIndexMap.getOrDefault("usagedate", 1);
        int serviceNameIdx = colIndexMap.getOrDefault("servicename", 2);
        int currencyIdx = colIndexMap.getOrDefault("currency", 3);

        for (JsonNode row : rows) {
            BigDecimal cost = new BigDecimal(row.get(costIdx).asText("0.0"));
            if (cost.compareTo(BigDecimal.ZERO) > 0) {
                int rawDate = row.get(usageDateIdx).asInt();
                LocalDate usageDate = parseAzureDateInt(rawDate);
                String serviceName = row.get(serviceNameIdx).asText("Unknown Azure Service");
                String currency = currencyIdx < row.size() ? row.get(currencyIdx).asText("USD") : "USD";

                records.add(NormalizedBillingRecord.builder()
                        .provider(CloudProvider.AZURE)
                        .serviceName(serviceName)
                        .region("eastus")
                        .costAmount(cost)
                        .currency(currency)
                        .usageType("ComputeHours")
                        .billingPeriodStart(usageDate)
                        .billingPeriodEnd(usageDate)
                        .sourceType(BillingSourceType.REAL_PROVIDER)
                        .build());
            }
        }

        return records;
    }

    @Override
    public ProviderCapabilities getCapabilities() {
        return ProviderCapabilities.builder()
                .provider(CloudProvider.AZURE)
                .providerName("Microsoft Azure")
                .supportedApi("Azure Cost Management Query API (v2023-11-01)")
                .reportingLag("8 to 24 hours (Azure Cost Management data latency)")
                .requiredPermissions(List.of("Microsoft.CostManagement/query/action", "Cost Management Reader"))
                .supportedDimensions(List.of("ServiceName", "ResourceGroupName", "ResourceType", "Location"))
                .liveSupported(true)
                .documentationUrl("https://learn.microsoft.com/en-us/rest/api/cost-management/query/usage")
                .authType("Microsoft Entra ID OAuth 2.0 (Service Principal Client Credentials)")
                .build();
    }

    private String acquireAccessToken(ProviderCredentials credentials) {
        String tokenUrl = "https://login.microsoftonline.com/" + credentials.getAzureTenantId() + "/oauth2/v2.0/token";

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("client_id", credentials.getAzureClientId());
        formData.add("client_secret", credentials.getAzureClientSecret());
        formData.add("scope", "https://management.azure.com/.default");

        try {
            JsonNode resp = restClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(JsonNode.class);

            return resp != null ? resp.path("access_token").asText() : null;
        } catch (HttpClientErrorException e) {
            log.error("Microsoft Entra ID token request failed [HTTP {}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }

    private JsonNode queryAzureCostManagement(
            ProviderCredentials credentials,
            String token,
            LocalDate startDate,
            LocalDate endDate) {

        String queryUrl = "https://management.azure.com/subscriptions/" + credentials.getAzureSubscriptionId() +
                "/providers/Microsoft.CostManagement/query?api-version=2023-11-01";

        Map<String, Object> queryPayload = new HashMap<>();
        queryPayload.put("type", "ActualCost");
        queryPayload.put("timeframe", "Custom");

        Map<String, String> timePeriod = new HashMap<>();
        timePeriod.put("from", startDate.toString() + "T00:00:00Z");
        timePeriod.put("to", endDate.toString() + "T23:59:59Z");
        queryPayload.put("timePeriod", timePeriod);

        Map<String, Object> dataset = new HashMap<>();
        dataset.put("granularity", "Daily");

        Map<String, Object> aggregation = new HashMap<>();
        Map<String, String> totalCost = new HashMap<>();
        totalCost.put("name", "Cost");
        totalCost.put("function", "Sum");
        aggregation.put("totalCost", totalCost);
        dataset.put("aggregation", aggregation);

        List<Map<String, String>> grouping = new ArrayList<>();
        Map<String, String> groupService = new HashMap<>();
        groupService.put("type", "Dimension");
        groupService.put("name", "ServiceName");
        grouping.add(groupService);
        dataset.put("grouping", grouping);

        queryPayload.put("dataset", dataset);

        return restClient.post()
                .uri(queryUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(queryPayload)
                .retrieve()
                .body(JsonNode.class);
    }

    private LocalDate parseAzureDateInt(int rawDate) {
        // Azure returns usage dates in YYYYMMDD integer format, e.g. 20261101
        try {
            return LocalDate.parse(String.valueOf(rawDate), DateTimeFormatter.BASIC_ISO_DATE);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private boolean isMockSecret(String secret) {
        return secret != null && (secret.startsWith("MOCK-") || secret.startsWith("TEST-") || secret.equals("mock-azure-secret"));
    }

    private List<NormalizedBillingRecord> generateMockAzureRecords(CloudAccount account, LocalDate start, LocalDate end) {
        List<NormalizedBillingRecord> mockRecords = new ArrayList<>();
        String[] services = {"Virtual Machines", "Azure App Service", "Azure Cosmos DB", "Azure Blob Storage"};
        BigDecimal[] costs = {new BigDecimal("51.2000"), new BigDecimal("18.4000"), new BigDecimal("29.9000"), new BigDecimal("9.5000")};

        for (int i = 0; i < services.length; i++) {
            mockRecords.add(NormalizedBillingRecord.builder()
                    .provider(CloudProvider.AZURE)
                    .providerRecordId("azure-mock-" + account.getAccountId() + "-" + (i + 1))
                    .serviceName(services[i])
                    .region("eastus")
                    .costAmount(costs[i])
                    .currency("USD")
                    .usageType("GeneralPurpose")
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
