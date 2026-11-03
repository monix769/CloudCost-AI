package com.cloudcostai.service;

import com.cloudcostai.dto.CostAnomalyResponse;
import com.cloudcostai.entity.AnomalySeverity;
import com.cloudcostai.entity.AnomalyStatus;
import com.cloudcostai.entity.CostAnomaly;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.exception.BadRequestException;
import com.cloudcostai.exception.ResourceNotFoundException;
import com.cloudcostai.mapper.CostAnomalyMapper;
import com.cloudcostai.repository.CostAnomalyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyService {

    private final CostAnomalyRepository costAnomalyRepository;
    private final CostAnomalyMapper costAnomalyMapper;
    private final OrganizationService organizationService;

    @Transactional(readOnly = true)
    public List<CostAnomalyResponse> getAnomalies(AnomalySeverity severity, AnomalyStatus status) {
        Organization org = organizationService.getDefaultOrganization();
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
    public CostAnomalyResponse getAnomalyById(Long id) {
        CostAnomaly anomaly = findAnomalyEntity(id);
        return costAnomalyMapper.toResponse(anomaly);
    }

    @Transactional
    public CostAnomalyResponse updateAnomalyStatus(Long id, AnomalyStatus newStatus) {
        if (newStatus == null) {
            throw new BadRequestException("Target anomaly status cannot be null");
        }

        CostAnomaly anomaly = findAnomalyEntity(id);
        AnomalyStatus currentStatus = anomaly.getStatus();

        // Validate state transitions
        if (currentStatus == AnomalyStatus.RESOLVED && newStatus == AnomalyStatus.OPEN) {
            throw new BadRequestException("Cannot revert a RESOLVED anomaly back to OPEN directly. Use ACKNOWLEDGED or create a new detection event.");
        }

        anomaly.setStatus(newStatus);
        CostAnomaly updated = costAnomalyRepository.save(anomaly);
        log.info("Transitioned anomaly [ID: {}] status from {} to {}", id, currentStatus, newStatus);
        return costAnomalyMapper.toResponse(updated);
    }

    @Transactional(readOnly = true)
    public CostAnomaly findAnomalyEntity(Long id) {
        Organization org = organizationService.getDefaultOrganization();
        return costAnomalyRepository.findByIdAndOrganizationId(id, org.getId())
                .orElseThrow(() -> new ResourceNotFoundException("CostAnomaly", "id", id));
    }
}
