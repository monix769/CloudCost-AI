package com.cloudcostai.service;

import com.cloudcostai.anomaly.service.AnomalyDetectionEngineService;
import com.cloudcostai.dto.AnomalyHistoryResponse;
import com.cloudcostai.dto.AnomalyKpiSummaryResponse;
import com.cloudcostai.dto.AnomalyTrendPointResponse;
import com.cloudcostai.dto.AnomalyTrendsResponse;
import com.cloudcostai.dto.CostAnomalyResponse;
import com.cloudcostai.entity.AnomalySeverity;
import com.cloudcostai.entity.AnomalyStatus;
import com.cloudcostai.entity.CloudProvider;
import com.cloudcostai.entity.CostAnomaly;
import com.cloudcostai.entity.CostAnomalyHistory;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.exception.BadRequestException;
import com.cloudcostai.exception.ResourceNotFoundException;
import com.cloudcostai.mapper.CostAnomalyMapper;
import com.cloudcostai.repository.CostAnomalyHistoryRepository;
import com.cloudcostai.repository.CostAnomalyRepository;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyService {

    private final CostAnomalyRepository costAnomalyRepository;
    private final CostAnomalyHistoryRepository historyRepository;
    private final CostAnomalyMapper costAnomalyMapper;
    private final OrganizationService organizationService;
    private final AnomalyDetectionEngineService detectionEngineService;

    @Transactional(readOnly = true)
    public List<CostAnomalyResponse> getAnomalies(AnomalySeverity severity, AnomalyStatus status) {
        Organization org = organizationService.getCurrentUserOrganization();
        List<CostAnomaly> anomalies;

        if (severity != null && status != null) {
            anomalies = costAnomalyRepository.findByOrganizationIdAndSeverityAndStatusOrderByDetectedAtDesc(
                    org.getId(), severity, status);
        } else if (severity != null) {
            anomalies = costAnomalyRepository.findByOrganizationIdAndSeverityOrderByDetectedAtDesc(
                    org.getId(), severity);
        } else if (status != null) {
            anomalies = costAnomalyRepository.findByOrganizationIdAndStatusOrderByDetectedAtDesc(
                    org.getId(), status);
        } else {
            anomalies = costAnomalyRepository.findByOrganizationIdOrderByDetectedAtDesc(org.getId());
        }

        return anomalies.stream()
                .map(costAnomalyMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<CostAnomalyResponse> getFilteredAnomaliesPaged(
            CloudProvider provider,
            String serviceName,
            AnomalySeverity severity,
            AnomalyStatus status,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {

        Organization org = organizationService.getCurrentUserOrganization();
        Instant startInstant = startDate != null ? startDate.atStartOfDay().toInstant(ZoneOffset.UTC) : null;
        Instant endInstant = endDate != null ? endDate.atTime(23, 59, 59).toInstant(ZoneOffset.UTC) : null;

        Page<CostAnomaly> paged = costAnomalyRepository.findFilteredAnomalies(
                org.getId(), severity, status, provider, serviceName, startInstant, endInstant, pageable);

        return paged.map(costAnomalyMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<CostAnomalyResponse> getFilteredAnomaliesList(
            CloudProvider provider,
            String serviceName,
            AnomalySeverity severity,
            AnomalyStatus status,
            LocalDate startDate,
            LocalDate endDate) {

        Organization org = organizationService.getCurrentUserOrganization();
        Instant startInstant = startDate != null ? startDate.atStartOfDay().toInstant(ZoneOffset.UTC) : null;
        Instant endInstant = endDate != null ? endDate.atTime(23, 59, 59).toInstant(ZoneOffset.UTC) : null;

        List<CostAnomaly> list = costAnomalyRepository.findFilteredAnomaliesList(
                org.getId(), severity, status, provider, serviceName, startInstant, endInstant);

        return list.stream()
                .map(costAnomalyMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CostAnomalyResponse getAnomalyById(Long id) {
        CostAnomaly anomaly = findAnomalyEntity(id);
        return costAnomalyMapper.toResponse(anomaly);
    }

    @Transactional(readOnly = true)
    public AnomalyKpiSummaryResponse getAnomalyKpiSummary(String currency) {
        Organization org = organizationService.getCurrentUserOrganization();
        Long orgId = org.getId();

        long total = costAnomalyRepository.countByOrganizationId(orgId);
        long critical = costAnomalyRepository.countByOrganizationIdAndSeverity(orgId, AnomalySeverity.CRITICAL);
        long high = costAnomalyRepository.countByOrganizationIdAndSeverity(orgId, AnomalySeverity.HIGH);
        long medium = costAnomalyRepository.countByOrganizationIdAndSeverity(orgId, AnomalySeverity.MEDIUM);
        long low = costAnomalyRepository.countByOrganizationIdAndSeverity(orgId, AnomalySeverity.LOW);

        long open = costAnomalyRepository.countByOrganizationIdAndStatus(orgId, AnomalyStatus.OPEN);
        long ack = costAnomalyRepository.countByOrganizationIdAndStatus(orgId, AnomalyStatus.ACKNOWLEDGED);
        long resolved = costAnomalyRepository.countByOrganizationIdAndStatus(orgId, AnomalyStatus.RESOLVED);
        long dismissed = costAnomalyRepository.countByOrganizationIdAndStatus(orgId, AnomalyStatus.DISMISSED) +
                         costAnomalyRepository.countByOrganizationIdAndStatus(orgId, AnomalyStatus.IGNORED);

        BigDecimal excessCost = costAnomalyRepository.sumActiveExcessCostByOrganizationId(orgId);
        String resolvedCurrency = currency != null && !currency.trim().isEmpty() ? currency.toUpperCase().trim() : "USD";

        return AnomalyKpiSummaryResponse.builder()
                .totalAnomalies(total)
                .criticalCount(critical)
                .highCount(high)
                .mediumCount(medium)
                .lowCount(low)
                .openCount(open)
                .acknowledgedCount(ack)
                .resolvedCount(resolved)
                .dismissedCount(dismissed)
                .totalExcessCost(excessCost != null ? excessCost : BigDecimal.ZERO)
                .currency(resolvedCurrency)
                .build();
    }

    @Transactional(readOnly = true)
    public AnomalyTrendsResponse getAnomalyTrends(LocalDate startDate, LocalDate endDate, String currency) {
        Organization org = organizationService.getCurrentUserOrganization();
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusDays(30);

        Instant startInstant = start.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endInstant = end.atTime(23, 59, 59).toInstant(ZoneOffset.UTC);

        List<CostAnomaly> anomalies = costAnomalyRepository.findByOrganizationIdAndDetectedAtBetweenOrderByDetectedAtAsc(
                org.getId(), startInstant, endInstant);

        // Group into daily buckets
        Map<LocalDate, List<CostAnomaly>> dayMap = new TreeMap<>();
        LocalDate cur = start;
        while (!cur.isAfter(end)) {
            dayMap.put(cur, new ArrayList<>());
            cur = cur.plusDays(1);
        }

        for (CostAnomaly a : anomalies) {
            LocalDate date = a.getDetectedAt().atZone(ZoneOffset.UTC).toLocalDate();
            if (dayMap.containsKey(date)) {
                dayMap.get(date).add(a);
            }
        }

        List<AnomalyTrendPointResponse> trendPoints = new ArrayList<>();
        for (Map.Entry<LocalDate, List<CostAnomaly>> entry : dayMap.entrySet()) {
            List<CostAnomaly> dayList = entry.getValue();
            int total = dayList.size();
            int critical = (int) dayList.stream().filter(a -> a.getSeverity() == AnomalySeverity.CRITICAL).count();
            int high = (int) dayList.stream().filter(a -> a.getSeverity() == AnomalySeverity.HIGH).count();
            BigDecimal excessSum = dayList.stream()
                    .map(a -> a.getExcessCost() != null ? a.getExcessCost() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            trendPoints.add(AnomalyTrendPointResponse.builder()
                    .date(entry.getKey())
                    .totalAnomalies(total)
                    .criticalCount(critical)
                    .highCount(high)
                    .excessCost(excessSum)
                    .build());
        }

        String resolvedCurrency = currency != null && !currency.trim().isEmpty() ? currency.toUpperCase().trim() : "USD";
        return AnomalyTrendsResponse.builder()
                .trends(trendPoints)
                .currency(resolvedCurrency)
                .build();
    }

    @Transactional
    public CostAnomalyResponse updateAnomalyStatus(Long id, AnomalyStatus newStatus) {
        return updateAnomalyStatus(id, newStatus, null);
    }

    @Transactional
    public CostAnomalyResponse updateAnomalyStatus(Long id, AnomalyStatus newStatus, String notes) {
        if (newStatus == null) {
            throw new BadRequestException("Target anomaly status cannot be null");
        }

        CostAnomaly anomaly = findAnomalyEntity(id);
        AnomalyStatus currentStatus = anomaly.getStatus();

        // Validate state transitions
        if (currentStatus == AnomalyStatus.RESOLVED && newStatus == AnomalyStatus.OPEN) {
            throw new BadRequestException("Cannot revert a RESOLVED anomaly back to OPEN directly. Use ACKNOWLEDGED or trigger a new detection scan.");
        }

        anomaly.setStatus(newStatus);
        CostAnomaly updated = costAnomalyRepository.save(anomaly);

        // Record status change event in history audit log
        Long operatorId = null;
        String operatorEmail = "OPERATOR";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            operatorId = principal.getId();
            operatorEmail = principal.getEmail();
        } else if (auth != null && auth.getName() != null) {
            operatorEmail = auth.getName();
        }

        CostAnomalyHistory history = CostAnomalyHistory.builder()
                .costAnomaly(updated)
                .previousStatus(currentStatus)
                .newStatus(newStatus)
                .actionByUserId(operatorId)
                .actionByUserEmail(operatorEmail)
                .notes(notes != null && !notes.trim().isEmpty() ? notes.trim() : "Status updated to " + newStatus)
                .createdAt(Instant.now())
                .build();
        historyRepository.save(history);

        log.info("Transitioned anomaly [ID: {}] status from {} to {} with notes: {}", id, currentStatus, newStatus, notes);
        return costAnomalyMapper.toResponse(updated);
    }

    @Transactional(readOnly = true)
    public List<AnomalyHistoryResponse> getAnomalyHistory(Long id) {
        // Ensure anomaly exists and belongs to current tenant
        CostAnomaly anomaly = findAnomalyEntity(id);

        List<CostAnomalyHistory> histories = historyRepository.findByCostAnomalyIdOrderByCreatedAtDesc(anomaly.getId());
        return histories.stream()
                .map(h -> AnomalyHistoryResponse.builder()
                        .id(h.getId())
                        .previousStatus(h.getPreviousStatus())
                        .newStatus(h.getNewStatus())
                        .actionByUserId(h.getActionByUserId())
                        .actionByUserEmail(h.getActionByUserEmail())
                        .notes(h.getNotes())
                        .createdAt(h.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public CostAnomaly findAnomalyEntity(Long id) {
        Organization org = organizationService.getCurrentUserOrganization();
        return costAnomalyRepository.findByIdAndOrganizationId(id, org.getId())
                .orElseThrow(() -> new ResourceNotFoundException("CostAnomaly", "id", id));
    }
}
