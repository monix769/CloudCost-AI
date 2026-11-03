package com.cloudcostai.repository;

import com.cloudcostai.entity.AnomalySeverity;
import com.cloudcostai.entity.AnomalyStatus;
import com.cloudcostai.entity.CostAnomaly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CostAnomalyRepository extends JpaRepository<CostAnomaly, Long> {

    List<CostAnomaly> findByOrganizationIdOrderByDetectedAtDesc(Long organizationId);

    List<CostAnomaly> findByOrganizationIdAndSeverityOrderByDetectedAtDesc(Long organizationId, AnomalySeverity severity);

    List<CostAnomaly> findByOrganizationIdAndStatusOrderByDetectedAtDesc(Long organizationId, AnomalyStatus status);

    List<CostAnomaly> findByOrganizationIdAndSeverityAndStatusOrderByDetectedAtDesc(Long organizationId, AnomalySeverity severity, AnomalyStatus status);

    Optional<CostAnomaly> findByIdAndOrganizationId(Long id, Long organizationId);
}
