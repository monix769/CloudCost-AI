package com.cloudcostai.service;

import com.cloudcostai.dto.CommitmentAnalysisResponse;
import com.cloudcostai.dto.OptimizationOverviewResponse;
import com.cloudcostai.dto.OptimizationRecommendationHistoryResponse;
import com.cloudcostai.dto.OptimizationRecommendationResponse;
import com.cloudcostai.dto.OptimizationStatusUpdateRequest;
import com.cloudcostai.dto.ResourceUtilizationResponse;
import com.cloudcostai.dto.SavingsSummaryResponse;
import com.cloudcostai.entity.CloudProvider;
import com.cloudcostai.entity.OptimizationPriority;
import com.cloudcostai.entity.OptimizationRecommendation;
import com.cloudcostai.entity.OptimizationRecommendationHistory;
import com.cloudcostai.entity.OptimizationRecommendationType;
import com.cloudcostai.entity.OptimizationStatus;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.entity.ResourceUtilizationSnapshot;
import com.cloudcostai.exception.BadRequestException;
import com.cloudcostai.exception.ResourceNotFoundException;
import com.cloudcostai.optimization.engine.CommitmentAnalysisEngineService;
import com.cloudcostai.optimization.engine.OptimizationEngineService;
import com.cloudcostai.optimization.engine.OptimizationExportService;
import com.cloudcostai.repository.OptimizationRecommendationHistoryRepository;
import com.cloudcostai.repository.OptimizationRecommendationRepository;
import com.cloudcostai.repository.ResourceUtilizationSnapshotRepository;
import com.cloudcostai.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OptimizationService {

    private final OptimizationRecommendationRepository recommendationRepository;
    private final OptimizationRecommendationHistoryRepository historyRepository;
    private final ResourceUtilizationSnapshotRepository snapshotRepository;
    private final OrganizationService organizationService;
    private final OptimizationEngineService optimizationEngineService;
    private final CommitmentAnalysisEngineService commitmentAnalysisEngineService;
    private final OptimizationExportService optimizationExportService;

    private static final BigDecimal MONTHS_PER_YEAR = new BigDecimal("12.00");
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    @Transactional(readOnly = true)
    public OptimizationOverviewResponse getOverview() {
        Organization org = organizationService.getCurrentUserOrganization();
        Long orgId = org.getId();

        BigDecimal potentialMonthly = recommendationRepository.sumPotentialMonthlySavings(orgId);
        BigDecimal potentialAnnual = recommendationRepository.sumPotentialAnnualSavings(orgId);
        BigDecimal realizedMonthly = recommendationRepository.sumRealizedMonthlySavings(orgId);
        BigDecimal realizedAnnual = recommendationRepository.sumRealizedAnnualSavings(orgId);

        long total = recommendationRepository.countByOrganizationId(orgId);
        long countNew = recommendationRepository.countByOrganizationIdAndStatus(orgId, OptimizationStatus.NEW);
        long countApproved = recommendationRepository.countByOrganizationIdAndStatus(orgId, OptimizationStatus.APPROVED);
        long countImplemented = recommendationRepository.countByOrganizationIdAndStatus(orgId, OptimizationStatus.IMPLEMENTED);

        long critical = recommendationRepository.countByOrganizationIdAndPriority(orgId, OptimizationPriority.CRITICAL);
        long high = recommendationRepository.countByOrganizationIdAndPriority(orgId, OptimizationPriority.HIGH);
        long medium = recommendationRepository.countByOrganizationIdAndPriority(orgId, OptimizationPriority.MEDIUM);
        long low = recommendationRepository.countByOrganizationIdAndPriority(orgId, OptimizationPriority.LOW);

        return OptimizationOverviewResponse.builder()
                .totalPotentialMonthlySavings(potentialMonthly != null ? potentialMonthly.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .totalPotentialAnnualSavings(potentialAnnual != null ? potentialAnnual.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .totalRealizedMonthlySavings(realizedMonthly != null ? realizedMonthly.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .totalRealizedAnnualSavings(realizedAnnual != null ? realizedAnnual.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .totalRecommendationsCount(total)
                .newRecommendationsCount(countNew)
                .approvedRecommendationsCount(countApproved)
                .implementedRecommendationsCount(countImplemented)
                .criticalPriorityCount(critical)
                .highPriorityCount(high)
                .mediumPriorityCount(medium)
                .lowPriorityCount(low)
                .currency("USD")
                .build();
    }

    @Transactional(readOnly = true)
    public Page<OptimizationRecommendationResponse> getRecommendations(
            CloudProvider provider,
            OptimizationRecommendationType type,
            OptimizationPriority priority,
            OptimizationStatus status,
            BigDecimal minSavings,
            String search,
            Pageable pageable) {

        Organization org = organizationService.getCurrentUserOrganization();
        String safeSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        Page<OptimizationRecommendation> page = recommendationRepository.findFilteredRecommendations(
                org.getId(), provider, type, priority, status, minSavings, safeSearch, pageable);

        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public OptimizationRecommendationResponse getRecommendationById(Long id) {
        OptimizationRecommendation rec = findRecommendationEntity(id);
        return toResponse(rec);
    }

    @Transactional
    public OptimizationRecommendationResponse updateStatus(Long id, OptimizationStatusUpdateRequest request) {
        if (request == null || request.getStatus() == null) {
            throw new BadRequestException("Target status is required");
        }

        OptimizationRecommendation rec = findRecommendationEntity(id);
        OptimizationStatus currentStatus = rec.getStatus();
        OptimizationStatus newStatus = request.getStatus();

        if (currentStatus == OptimizationStatus.IMPLEMENTED && newStatus == OptimizationStatus.NEW) {
            throw new BadRequestException("Cannot revert an IMPLEMENTED recommendation back to NEW directly.");
        }

        rec.setStatus(newStatus);
        if (request.getNotes() != null && !request.getNotes().trim().isEmpty()) {
            rec.setImplementationNotes(request.getNotes().trim());
        }

        if (newStatus == OptimizationStatus.IMPLEMENTED) {
            BigDecimal realizedMo = (request.getRealizedMonthlySavings() != null && request.getRealizedMonthlySavings().compareTo(BigDecimal.ZERO) > 0)
                    ? request.getRealizedMonthlySavings()
                    : rec.getEstimatedMonthlySavings();
            rec.setRealizedMonthlySavings(realizedMo);
            rec.setRealizedAnnualSavings(realizedMo.multiply(MONTHS_PER_YEAR).setScale(4, RoundingMode.HALF_UP));
        }

        rec.setUpdatedAt(Instant.now());
        OptimizationRecommendation updated = recommendationRepository.save(rec);

        // Record history audit log
        Long operatorId = null;
        String operatorEmail = "OPERATOR";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            operatorId = principal.getId();
            operatorEmail = principal.getEmail();
        } else if (auth != null && auth.getName() != null) {
            operatorEmail = auth.getName();
        }

        OptimizationRecommendationHistory history = OptimizationRecommendationHistory.builder()
                .recommendation(updated)
                .previousStatus(currentStatus)
                .newStatus(newStatus)
                .actionByUserId(operatorId)
                .actionByUserEmail(operatorEmail)
                .notes(request.getNotes() != null && !request.getNotes().trim().isEmpty() ? request.getNotes().trim() : "Status updated to " + newStatus)
                .createdAt(Instant.now())
                .build();
        historyRepository.save(history);

        log.info("Transitioned recommendation [ID: {}] from {} to {} by [{}]", id, currentStatus, newStatus, operatorEmail);
        return toResponse(updated);
    }

    @Transactional(readOnly = true)
    public List<OptimizationRecommendationHistoryResponse> getRecommendationHistory(Long id) {
        OptimizationRecommendation rec = findRecommendationEntity(id);
        List<OptimizationRecommendationHistory> list = historyRepository.findByRecommendationIdOrderByCreatedAtDesc(rec.getId());
        return list.stream()
                .map(h -> OptimizationRecommendationHistoryResponse.builder()
                        .id(h.getId())
                        .recommendationId(rec.getId())
                        .previousStatus(h.getPreviousStatus() != null ? h.getPreviousStatus().name() : null)
                        .newStatus(h.getNewStatus().name())
                        .actionByUserId(h.getActionByUserId())
                        .actionByUserEmail(h.getActionByUserEmail())
                        .notes(h.getNotes())
                        .createdAt(h.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public SavingsSummaryResponse getSavingsSummary() {
        Organization org = organizationService.getCurrentUserOrganization();
        Long orgId = org.getId();

        BigDecimal potentialMonthly = recommendationRepository.sumPotentialMonthlySavings(orgId);
        BigDecimal potentialAnnual = recommendationRepository.sumPotentialAnnualSavings(orgId);
        BigDecimal realizedMonthly = recommendationRepository.sumRealizedMonthlySavings(orgId);
        BigDecimal realizedAnnual = recommendationRepository.sumRealizedAnnualSavings(orgId);

        Map<String, BigDecimal> byProvider = new HashMap<>();
        List<Object[]> providerRows = recommendationRepository.sumMonthlySavingsByProvider(orgId);
        for (Object[] row : providerRows) {
            CloudProvider p = (CloudProvider) row[0];
            BigDecimal s = (BigDecimal) row[1];
            byProvider.put(p != null ? p.name() : "OTHER", s != null ? s.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        }

        Map<String, BigDecimal> byType = new HashMap<>();
        List<Object[]> typeRows = recommendationRepository.sumMonthlySavingsByType(orgId);
        for (Object[] row : typeRows) {
            OptimizationRecommendationType t = (OptimizationRecommendationType) row[0];
            BigDecimal s = (BigDecimal) row[1];
            byType.put(t != null ? t.name() : "OTHER", s != null ? s.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        }

        Map<String, BigDecimal> byResourceType = new HashMap<>();
        List<Object[]> resourceRows = recommendationRepository.sumMonthlySavingsByResourceType(orgId);
        for (Object[] row : resourceRows) {
            String rt = (String) row[0];
            BigDecimal s = (BigDecimal) row[1];
            byResourceType.put(rt != null ? rt : "UNKNOWN", s != null ? s.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        }

        return SavingsSummaryResponse.builder()
                .totalPotentialMonthlySavings(potentialMonthly != null ? potentialMonthly.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .totalPotentialAnnualSavings(potentialAnnual != null ? potentialAnnual.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .totalRealizedMonthlySavings(realizedMonthly != null ? realizedMonthly.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .totalRealizedAnnualSavings(realizedAnnual != null ? realizedAnnual.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .byProvider(byProvider)
                .byRecommendationType(byType)
                .byResourceType(byResourceType)
                .currency("USD")
                .build();
    }

    @Transactional(readOnly = true)
    public Page<ResourceUtilizationResponse> getResourceUtilization(Pageable pageable) {
        Organization org = organizationService.getCurrentUserOrganization();
        Page<ResourceUtilizationSnapshot> page = snapshotRepository.findByOrganizationId(org.getId(), pageable);

        return page.map(s -> {
            String status = "HEALTHY";
            if (s.getCpuUtilizationAvg() == null) {
                status = "MONITORING_REQUIRED";
            } else if (s.getCpuUtilizationAvg().compareTo(new BigDecimal("2.50")) < 0) {
                status = "IDLE";
            } else if (s.getCpuUtilizationAvg().compareTo(new BigDecimal("15.00")) < 0) {
                status = "UNDERUTILIZED";
            }

            return ResourceUtilizationResponse.builder()
                    .id(s.getId())
                    .cloudAccountId(s.getCloudAccount() != null ? s.getCloudAccount().getId() : null)
                    .cloudAccountName(s.getCloudAccount() != null ? s.getCloudAccount().getAccountName() : null)
                    .cloudProvider(s.getCloudAccount() != null && s.getCloudAccount().getProvider() != null ? s.getCloudAccount().getProvider().name() : "AWS")
                    .resourceId(s.getResourceId())
                    .resourceName(s.getResourceName())
                    .resourceType(s.getResourceType())
                    .region(s.getRegion())
                    .cpuUtilizationAvg(s.getCpuUtilizationAvg())
                    .cpuUtilizationMax(s.getCpuUtilizationMax())
                    .memoryUtilizationAvg(s.getMemoryUtilizationAvg())
                    .memoryUtilizationMax(s.getMemoryUtilizationMax())
                    .runningHours(s.getRunningHours())
                    .snapshotDate(s.getSnapshotDate())
                    .telemetryStatus(status)
                    .build();
        });
    }

    @Transactional(readOnly = true)
    public CommitmentAnalysisResponse getCommitmentAnalysis() {
        Organization org = organizationService.getCurrentUserOrganization();
        return commitmentAnalysisEngineService.analyzeCommitment(org);
    }

    @Transactional
    public Map<String, Object> runAnalysis() {
        Organization org = organizationService.getCurrentUserOrganization();
        return optimizationEngineService.runOptimizationAnalysis(org);
    }

    @Transactional(readOnly = true)
    public String exportCsv() {
        Organization org = organizationService.getCurrentUserOrganization();
        return optimizationExportService.exportToCsv(org.getId());
    }

    @Transactional(readOnly = true)
    public OptimizationRecommendation findRecommendationEntity(Long id) {
        Organization org = organizationService.getCurrentUserOrganization();
        return recommendationRepository.findByIdAndOrganizationId(id, org.getId())
                .orElseThrow(() -> new ResourceNotFoundException("OptimizationRecommendation", "id", id));
    }

    private OptimizationRecommendationResponse toResponse(OptimizationRecommendation r) {
        if (r == null) return null;

        BigDecimal savingsPercent = BigDecimal.ZERO;
        if (r.getCurrentMonthlyCost() != null && r.getCurrentMonthlyCost().compareTo(BigDecimal.ZERO) > 0
                && r.getEstimatedMonthlySavings() != null) {
            savingsPercent = r.getEstimatedMonthlySavings()
                    .divide(r.getCurrentMonthlyCost(), 4, RoundingMode.HALF_UP)
                    .multiply(HUNDRED)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // Check for latest telemetry snapshot
        Optional<ResourceUtilizationSnapshot> snapOpt = snapshotRepository.findTopByOrganizationIdAndResourceIdOrderBySnapshotDateDesc(
                r.getOrganization().getId(), r.getResourceId());

        BigDecimal cpuAvg = null;
        BigDecimal cpuMax = null;
        BigDecimal memAvg = null;
        BigDecimal memMax = null;
        boolean hasTelemetry = false;

        if (snapOpt.isPresent()) {
            ResourceUtilizationSnapshot s = snapOpt.get();
            cpuAvg = s.getCpuUtilizationAvg();
            cpuMax = s.getCpuUtilizationMax();
            memAvg = s.getMemoryUtilizationAvg();
            memMax = s.getMemoryUtilizationMax();
            hasTelemetry = (cpuAvg != null || memAvg != null);
        }

        return OptimizationRecommendationResponse.builder()
                .id(r.getId())
                .cloudAccountId(r.getCloudAccount() != null ? r.getCloudAccount().getId() : null)
                .cloudAccountName(r.getCloudAccount() != null ? r.getCloudAccount().getAccountName() : null)
                .cloudProvider(r.getCloudProvider() != null ? r.getCloudProvider().name() : null)
                .resourceId(r.getResourceId())
                .resourceName(r.getResourceName())
                .resourceType(r.getResourceType())
                .recommendationType(r.getRecommendationType() != null ? r.getRecommendationType().name() : null)
                .currentConfig(r.getCurrentConfig())
                .suggestedConfig(r.getSuggestedConfig())
                .priority(r.getPriority() != null ? r.getPriority().name() : null)
                .status(r.getStatus() != null ? r.getStatus().name() : null)
                .confidenceLevel(r.getConfidenceLevel() != null ? r.getConfidenceLevel().name() : null)
                .reason(r.getReason())
                .currentMonthlyCost(r.getCurrentMonthlyCost())
                .recommendedMonthlyCost(r.getRecommendedMonthlyCost())
                .estimatedMonthlySavings(r.getEstimatedMonthlySavings())
                .estimatedAnnualSavings(r.getEstimatedAnnualSavings())
                .realizedMonthlySavings(r.getRealizedMonthlySavings())
                .realizedAnnualSavings(r.getRealizedAnnualSavings())
                .savingsPercentage(savingsPercent)
                .currency(r.getCurrency())
                .implementationNotes(r.getImplementationNotes())
                .detectedAt(r.getDetectedAt())
                .updatedAt(r.getUpdatedAt())
                .cpuUtilizationAvg(cpuAvg)
                .cpuUtilizationMax(cpuMax)
                .memoryUtilizationAvg(memAvg)
                .memoryUtilizationMax(memMax)
                .hasTelemetryData(hasTelemetry)
                .build();
    }
}

