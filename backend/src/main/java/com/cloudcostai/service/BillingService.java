package com.cloudcostai.service;

import com.cloudcostai.dto.BillingRecordRequest;
import com.cloudcostai.dto.BillingRecordResponse;
import com.cloudcostai.dto.BillingUploadResponse;
import com.cloudcostai.entity.BillingRecord;
import com.cloudcostai.entity.CloudAccount;
import com.cloudcostai.entity.CloudProvider;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.exception.BadRequestException;
import com.cloudcostai.exception.ResourceNotFoundException;
import com.cloudcostai.mapper.BillingRecordMapper;
import com.cloudcostai.repository.BillingRecordRepository;
import com.cloudcostai.repository.CloudAccountRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingService {

    private final BillingRecordRepository billingRecordRepository;
    private final CloudAccountRepository cloudAccountRepository;
    private final BillingRecordMapper billingRecordMapper;
    private final OrganizationService organizationService;

    private static final DateTimeFormatter[] DATE_FORMATTERS = new DateTimeFormatter[] {
            DateTimeFormatter.ISO_LOCAL_DATE, // YYYY-MM-DD
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy")
    };

    @Transactional(readOnly = true)
    public Page<BillingRecordResponse> getBillingRecords(
            Long cloudAccountId,
            CloudProvider provider,
            String serviceName,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {

        Organization org = organizationService.getCurrentUserOrganization();

        Specification<BillingRecord> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organization").get("id"), org.getId()));

            if (cloudAccountId != null) {
                predicates.add(cb.equal(root.get("cloudAccount").get("id"), cloudAccountId));
            }
            if (provider != null) {
                predicates.add(cb.equal(root.get("cloudAccount").get("provider"), provider));
            }
            if (serviceName != null && !serviceName.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("serviceName")), "%" + serviceName.trim().toLowerCase() + "%"));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("billingPeriodStart"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("billingPeriodEnd"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return billingRecordRepository.findAll(spec, pageable)
                .map(billingRecordMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public BillingRecordResponse getBillingRecordById(Long id) {
        Organization org = organizationService.getCurrentUserOrganization();
        BillingRecord record = billingRecordRepository.findByIdAndOrganizationId(id, org.getId())
                .orElseThrow(() -> new ResourceNotFoundException("BillingRecord", "id", id));
        return billingRecordMapper.toResponse(record);
    }

    @Transactional
    public BillingRecordResponse createBillingRecord(BillingRecordRequest request) {
        Organization org = organizationService.getCurrentUserOrganization();

        if (request.getBillingPeriodStart().isAfter(request.getBillingPeriodEnd())) {
            throw new BadRequestException("Billing period start date must not be after end date");
        }

        CloudAccount account = cloudAccountRepository.findByIdAndOrganizationId(request.getCloudAccountId(), org.getId())
                .orElseThrow(() -> new ResourceNotFoundException("CloudAccount", "id", request.getCloudAccountId()));

        BillingRecord record = billingRecordMapper.toEntity(request, org, account);
        BillingRecord saved = billingRecordRepository.save(record);
        log.info("Created billing record [ID: {}, Service: {}, Cost: {}]", saved.getId(), saved.getServiceName(), saved.getCostAmount());
        return billingRecordMapper.toResponse(saved);
    }

    @Transactional
    public void deleteBillingRecord(Long id) {
        Organization org = organizationService.getCurrentUserOrganization();
        BillingRecord record = billingRecordRepository.findByIdAndOrganizationId(id, org.getId())
                .orElseThrow(() -> new ResourceNotFoundException("BillingRecord", "id", id));
        billingRecordRepository.delete(record);
        log.info("Deleted billing record [ID: {}]", id);
    }

    @Transactional
    public BillingUploadResponse processCsvUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded CSV file cannot be empty");
        }

        String filename = file.getOriginalFilename();
        if (filename != null && !filename.toLowerCase().endsWith(".csv")) {
            throw new BadRequestException("Only valid .csv files are supported for billing ingestion");
        }

        Organization org = organizationService.getCurrentUserOrganization();

        int totalProcessed = 0;
        int importedCount = 0;
        int duplicateCount = 0;
        int failedCount = 0;
        List<String> errors = new ArrayList<>();
        List<BillingRecord> recordsToSave = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            for (CSVRecord record : csvParser) {
                totalProcessed++;
                long rowNumber = record.getRecordNumber() + 1; // 1-indexed including header

                try {
                    String providerStr = getHeaderValue(record, "provider", "cloud_provider");
                    if (providerStr == null || providerStr.trim().isEmpty()) {
                        throw new IllegalArgumentException("Missing required column 'provider'");
                    }
                    CloudProvider provider = CloudProvider.fromString(providerStr);

                    String accountId = getHeaderValue(record, "account_id", "accountId", "account");
                    if (accountId == null || accountId.trim().isEmpty()) {
                        throw new IllegalArgumentException("Missing required column 'account_id'");
                    }

                    String serviceName = getHeaderValue(record, "service_name", "service", "serviceName");
                    if (serviceName == null || serviceName.trim().isEmpty()) {
                        throw new IllegalArgumentException("Missing required column 'service_name'");
                    }

                    String region = getHeaderValue(record, "region");
                    if (region == null || region.trim().isEmpty()) {
                        region = "global";
                    }

                    String costStr = getHeaderValue(record, "cost_amount", "cost", "amount", "costAmount");
                    if (costStr == null || costStr.trim().isEmpty()) {
                        throw new IllegalArgumentException("Missing required column 'cost_amount'");
                    }
                    BigDecimal costAmount;
                    try {
                        costAmount = new BigDecimal(costStr.replace("$", "").replace(",", "").trim());
                        if (costAmount.compareTo(BigDecimal.ZERO) < 0) {
                            throw new IllegalArgumentException("Cost amount cannot be negative");
                        }
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid monetary decimal in 'cost_amount': " + costStr);
                    }

                    String currency = getHeaderValue(record, "currency");
                    if (currency == null || currency.trim().isEmpty()) {
                        currency = "USD";
                    }

                    String startStr = getHeaderValue(record, "billing_period_start", "start_date", "startDate");
                    String endStr = getHeaderValue(record, "billing_period_end", "end_date", "endDate");
                    if (startStr == null || startStr.trim().isEmpty() || endStr == null || endStr.trim().isEmpty()) {
                        throw new IllegalArgumentException("Missing billing period dates ('billing_period_start', 'billing_period_end')");
                    }

                    LocalDate startDate = parseDate(startStr);
                    LocalDate endDate = parseDate(endStr);
                    if (startDate.isAfter(endDate)) {
                        throw new IllegalArgumentException("billing_period_start (" + startDate + ") cannot be after billing_period_end (" + endDate + ")");
                    }

                    String usageType = getHeaderValue(record, "usage_type", "usageType");
                    String usageQtyStr = getHeaderValue(record, "usage_quantity", "usageQuantity", "quantity");
                    BigDecimal usageQuantity = null;
                    if (usageQtyStr != null && !usageQtyStr.trim().isEmpty()) {
                        try {
                            usageQuantity = new BigDecimal(usageQtyStr.trim());
                        } catch (NumberFormatException ignored) {}
                    }
                    String usageUnit = getHeaderValue(record, "usage_unit", "usageUnit", "unit");
                    String resourceId = getHeaderValue(record, "resource_id", "resourceId");
                    String tags = getHeaderValue(record, "tags");

                    // Locate or resolve CloudAccount
                    CloudAccount cloudAccount = cloudAccountRepository.findByOrganizationIdAndProviderAndAccountId(
                            org.getId(), provider, accountId.trim())
                            .orElseGet(() -> {
                                log.info("Auto-registering referenced CloudAccount from CSV [Provider: {}, AccountId: {}]", provider, accountId);
                                CloudAccount newAcc = CloudAccount.builder()
                                        .organization(org)
                                        .provider(provider)
                                        .accountId(accountId.trim())
                                        .accountName(provider.name() + " Account (" + accountId.trim() + ")")
                                        .syncStatus("ACTIVE")
                                        .isActive(true)
                                        .build();
                                return cloudAccountRepository.save(newAcc);
                            });

                    // Check duplicate
                    String fingerprint = com.cloudcostai.cloud.util.BillingFingerprintGenerator.computeFingerprint(
                            org.getId(), cloudAccount.getId(), provider,
                            com.cloudcostai.cloud.provider.NormalizedBillingRecord.builder()
                                    .serviceName(serviceName.trim())
                                    .region(region.trim())
                                    .costAmount(costAmount)
                                    .currency(currency.trim().toUpperCase())
                                    .usageType(usageType != null ? usageType.trim() : null)
                                    .usageQuantity(usageQuantity)
                                    .usageUnit(usageUnit != null ? usageUnit.trim() : null)
                                    .billingPeriodStart(startDate)
                                    .billingPeriodEnd(endDate)
                                    .resourceId(resourceId != null ? resourceId.trim() : null)
                                    .build());

                    boolean exists = billingRecordRepository.existsByOrganizationIdAndCloudAccountIdAndExternalRecordId(
                            org.getId(), cloudAccount.getId(), fingerprint) ||
                            billingRecordRepository.existsByOrganizationIdAndCloudAccountIdAndServiceNameAndBillingPeriodStartAndBillingPeriodEndAndCostAmount(
                                    org.getId(), cloudAccount.getId(), serviceName.trim(), startDate, endDate, costAmount);

                    if (exists) {
                        duplicateCount++;
                        continue;
                    }

                    BillingRecord billingRecord = BillingRecord.builder()
                            .organization(org)
                            .cloudAccount(cloudAccount)
                            .serviceName(serviceName.trim())
                            .region(region.trim())
                            .costAmount(costAmount)
                            .currency(currency.trim().toUpperCase())
                            .usageType(usageType != null ? usageType.trim() : null)
                            .usageQuantity(usageQuantity)
                            .usageUnit(usageUnit != null ? usageUnit.trim() : null)
                            .billingPeriodStart(startDate)
                            .billingPeriodEnd(endDate)
                            .resourceId(resourceId != null ? resourceId.trim() : null)
                            .tags(tags != null ? tags.trim() : null)
                            .sourceType(com.cloudcostai.entity.BillingSourceType.CSV_UPLOAD)
                            .externalRecordId(fingerprint)
                            .build();

                    recordsToSave.add(billingRecord);
                    importedCount++;

                } catch (Exception e) {
                    failedCount++;
                    errors.add("Row " + rowNumber + ": " + e.getMessage());
                }
            }

            if (!recordsToSave.isEmpty()) {
                billingRecordRepository.saveAll(recordsToSave);
                log.info("Batch saved {} billing records from CSV upload", recordsToSave.size());
            }

        } catch (Exception e) {
            log.error("Failed to parse uploaded CSV file: ", e);
            throw new BadRequestException("Malformed CSV structure or file read error: " + e.getMessage());
        }

        String summary = String.format("Processed %d rows: %d imported successfully, %d duplicates skipped, %d failed.",
                totalProcessed, importedCount, duplicateCount, failedCount);

        return BillingUploadResponse.builder()
                .totalProcessed(totalProcessed)
                .importedCount(importedCount)
                .duplicateCount(duplicateCount)
                .failedCount(failedCount)
                .errors(errors)
                .summary(summary)
                .build();
    }

    private String getHeaderValue(CSVRecord record, String... headerAliases) {
        for (String alias : headerAliases) {
            if (record.isMapped(alias)) {
                String val = record.get(alias);
                if (val != null) {
                    return val.trim();
                }
            }
        }
        return null;
    }

    private LocalDate parseDate(String dateStr) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateStr.trim(), formatter);
            } catch (DateTimeParseException ignored) {}
        }
        throw new IllegalArgumentException("Unparseable date format: '" + dateStr + "'. Expected format: YYYY-MM-DD");
    }
}
