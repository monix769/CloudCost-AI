package com.cloudcostai.cloud.util;

import com.cloudcostai.cloud.provider.NormalizedBillingRecord;
import com.cloudcostai.entity.CloudProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BillingFingerprintGeneratorTest {

    @Test
    @DisplayName("Should use provider-native record ID when available")
    void shouldUseProviderNativeIdWhenAvailable() {
        NormalizedBillingRecord record = NormalizedBillingRecord.builder()
                .providerRecordId("line-item-12345-native")
                .serviceName("Amazon EC2")
                .costAmount(new BigDecimal("100.00"))
                .build();

        String fingerprint = BillingFingerprintGenerator.computeFingerprint(1L, 10L, CloudProvider.AWS, record);
        assertThat(fingerprint).isEqualTo("aws:line-item-12345-native");
    }

    @Test
    @DisplayName("Should generate deterministic SHA-256 fingerprint when provider ID is absent")
    void shouldGenerateDeterministicFingerprint() {
        NormalizedBillingRecord rec1 = NormalizedBillingRecord.builder()
                .serviceName("Virtual Machines")
                .region("eastus")
                .costAmount(new BigDecimal("45.5000"))
                .currency("USD")
                .usageType("ComputeHours")
                .billingPeriodStart(LocalDate.of(2026, 5, 1))
                .billingPeriodEnd(LocalDate.of(2026, 5, 1))
                .build();

        NormalizedBillingRecord rec2 = NormalizedBillingRecord.builder()
                .serviceName("Virtual Machines")
                .region("eastus")
                .costAmount(new BigDecimal("45.50")) // Different trailing zeros
                .currency("USD")
                .usageType("ComputeHours")
                .billingPeriodStart(LocalDate.of(2026, 5, 1))
                .billingPeriodEnd(LocalDate.of(2026, 5, 1))
                .build();

        String fp1 = BillingFingerprintGenerator.computeFingerprint(1L, 10L, CloudProvider.AZURE, rec1);
        String fp2 = BillingFingerprintGenerator.computeFingerprint(1L, 10L, CloudProvider.AZURE, rec2);

        assertThat(fp1).startsWith("fp:");
        assertThat(fp1).isEqualTo(fp2); // Must be strictly identical
    }

    @Test
    @DisplayName("Should generate different fingerprints when cost or date differs")
    void shouldGenerateDifferentFingerprintsForDifferentDimensions() {
        NormalizedBillingRecord rec1 = NormalizedBillingRecord.builder()
                .serviceName("Compute Engine")
                .region("us-central1")
                .costAmount(new BigDecimal("25.00"))
                .currency("USD")
                .billingPeriodStart(LocalDate.of(2026, 5, 1))
                .billingPeriodEnd(LocalDate.of(2026, 5, 1))
                .build();

        NormalizedBillingRecord rec2 = NormalizedBillingRecord.builder()
                .serviceName("Compute Engine")
                .region("us-central1")
                .costAmount(new BigDecimal("26.00")) // Different cost
                .currency("USD")
                .billingPeriodStart(LocalDate.of(2026, 5, 1))
                .billingPeriodEnd(LocalDate.of(2026, 5, 1))
                .build();

        String fp1 = BillingFingerprintGenerator.computeFingerprint(1L, 10L, CloudProvider.GCP, rec1);
        String fp2 = BillingFingerprintGenerator.computeFingerprint(1L, 10L, CloudProvider.GCP, rec2);

        assertThat(fp1).isNotEqualTo(fp2);
    }
}
