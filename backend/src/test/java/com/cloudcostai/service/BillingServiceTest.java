package com.cloudcostai.service;

import com.cloudcostai.dto.BillingUploadResponse;
import com.cloudcostai.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class BillingServiceTest {

    @Autowired
    private BillingService billingService;

    @Test
    @DisplayName("processCsvUpload - Valid CSV rows should be imported")
    void shouldImportValidCsvRows() {
        String csv = """
                provider,account_id,service_name,region,cost_amount,currency,usage_type,usage_quantity,usage_unit,billing_period_start,billing_period_end,resource_id
                AWS,123456789012,Amazon DynamoDB,us-east-1,12.50,USD,ReadCapacity,500.0,Units,2026-05-01,2026-05-31,table-test-01
                AZURE,sub-azure-5544-22,Virtual Machines,eastus,88.20,USD,Standard_D2s_v3,720.0,Hours,2026-05-01,2026-05-31,vm-azure-01
                """;

        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        BillingUploadResponse response = billingService.processCsvUpload(file);

        assertThat(response.getTotalProcessed()).isEqualTo(2);
        assertThat(response.getImportedCount()).isGreaterThanOrEqualTo(1);
        assertThat(response.getFailedCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("processCsvUpload - Empty file should throw BadRequestException")
    void shouldThrowOnEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);
        assertThrows(BadRequestException.class, () -> billingService.processCsvUpload(emptyFile));
    }

    @Test
    @DisplayName("processCsvUpload - Malformed records should be captured in errors list")
    void shouldCaptureRowErrorsForMalformedData() {
        String csvWithErrors = """
                provider,account_id,service_name,region,cost_amount,currency,billing_period_start,billing_period_end
                UNKNOWN_CLOUD,123,EC2,us-east-1,10.00,USD,2026-05-01,2026-05-31
                AWS,123456789012,EC2,us-east-1,NOT_A_NUMBER,USD,2026-05-01,2026-05-31
                AWS,123456789012,EC2,us-east-1,50.00,USD,2026-06-30,2026-06-01
                """;

        MockMultipartFile file = new MockMultipartFile("file", "errors.csv", "text/csv", csvWithErrors.getBytes(StandardCharsets.UTF_8));
        BillingUploadResponse response = billingService.processCsvUpload(file);

        assertThat(response.getTotalProcessed()).isEqualTo(3);
        assertThat(response.getFailedCount()).isEqualTo(3);
        assertThat(response.getErrors()).hasSize(3);
        assertThat(response.getErrors().get(0)).contains("Unknown cloud provider");
        assertThat(response.getErrors().get(1)).contains("Invalid monetary decimal");
        assertThat(response.getErrors().get(2)).contains("cannot be after");
    }
}
