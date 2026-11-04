package com.cloudcostai.cloud.util;

import com.cloudcostai.cloud.provider.NormalizedBillingRecord;
import com.cloudcostai.entity.CloudProvider;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;

/**
 * Generates deterministic idempotency fingerprints for cloud billing line items.
 * Adheres to Requirement 1:
 * "Prefer stable provider-native record identifiers. If unavailable, construct a deterministic
 * fingerprint using sufficient billing dimensions, including resource ID, SKU, usage type,
 * region, usage quantity and billing period where available."
 */
public final class BillingFingerprintGenerator {

    private BillingFingerprintGenerator() {}

    public static String computeFingerprint(
            Long organizationId,
            Long cloudAccountId,
            CloudProvider provider,
            NormalizedBillingRecord record) {

        // 1. If provider supplies a stable native record ID, prefix and return it directly
        if (record.getProviderRecordId() != null && !record.getProviderRecordId().trim().isEmpty()) {
            return provider.name().toLowerCase() + ":" + record.getProviderRecordId().trim();
        }

        // 2. Otherwise, construct a deterministic composite across all available billing dimensions
        String orgIdStr = String.valueOf(organizationId);
        String accIdStr = String.valueOf(cloudAccountId);
        String provStr = provider.name();
        String service = safeTrim(record.getServiceName());
        String region = safeTrim(record.getRegion());
        String start = record.getBillingPeriodStart() != null ? record.getBillingPeriodStart().toString() : "";
        String end = record.getBillingPeriodEnd() != null ? record.getBillingPeriodEnd().toString() : "";
        String cost = formatDecimal(record.getCostAmount());
        String currency = safeTrim(record.getCurrency());
        String resourceId = safeTrim(record.getResourceId());
        String usageType = safeTrim(record.getUsageType());
        String usageQuantity = formatDecimal(record.getUsageQuantity());
        String usageUnit = safeTrim(record.getUsageUnit());

        String rawComposite = String.join("|",
                orgIdStr, accIdStr, provStr, service, region, start, end, cost, currency,
                resourceId, usageType, usageQuantity, usageUnit);

        return "fp:" + sha256Hex(rawComposite);
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String formatDecimal(BigDecimal d) {
        if (d == null) return "0";
        return d.stripTrailingZeros().toPlainString();
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable in JVM", e);
        }
    }
}
