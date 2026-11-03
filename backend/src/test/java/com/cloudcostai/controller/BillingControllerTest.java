package com.cloudcostai.controller;

import com.cloudcostai.dto.BillingRecordRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class BillingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/billing - Should return paginated billing records")
    void shouldReturnPaginatedBillingRecords() throws Exception {
        mockMvc.perform(get("/api/billing?page=0&size=10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", notNullValue()))
                .andExpect(jsonPath("$.data.pageable", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/billing - Should reject invalid date range (start after end)")
    void shouldRejectInvalidDateRange() throws Exception {
        BillingRecordRequest request = BillingRecordRequest.builder()
                .cloudAccountId(1L)
                .serviceName("Amazon EC2")
                .region("us-east-1")
                .costAmount(new BigDecimal("150.00"))
                .billingPeriodStart(LocalDate.of(2026, 6, 30))
                .billingPeriodEnd(LocalDate.of(2026, 6, 1)) // Invalid: start after end
                .build();

        mockMvc.perform(post("/api/billing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Billing period start date must not be after end date")));
    }

    @Test
    @DisplayName("POST /api/billing/upload - Should parse CSV file and import valid records")
    void shouldUploadAndProcessBillingCsv() throws Exception {
        String csvContent = """
                provider,account_id,service_name,region,cost_amount,currency,usage_type,usage_quantity,usage_unit,billing_period_start,billing_period_end,resource_id
                AWS,123456789012,Amazon S3,us-east-1,45.2500,USD,StandardStorage,1200.00,GB-Mo,2026-05-01,2026-05-31,arn:aws:s3:::my-test-bucket
                GCP,gcp-production-9876,Compute Engine,us-central1,95.5000,USD,n1-standard-4,720.00,Hours,2026-05-01,2026-05-31,inst-gcp-001
                """;

        MockMultipartFile csvFile = new MockMultipartFile(
                "file",
                "billing_sample.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/billing/upload").file(csvFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalProcessed", is(2)))
                .andExpect(jsonPath("$.data.importedCount", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.data.summary", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/billing/upload - Should reject non-csv files")
    void shouldRejectNonCsvFiles() throws Exception {
        MockMultipartFile txtFile = new MockMultipartFile(
                "file",
                "invoice.pdf",
                "application/pdf",
                "binary fake data".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/billing/upload").file(txtFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString(".csv files are supported")));
    }
}
