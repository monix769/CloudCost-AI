package com.cloudcostai.repository;

import com.cloudcostai.entity.AnomalySeverity;
import com.cloudcostai.entity.AnomalyStatus;
import com.cloudcostai.entity.CloudProvider;
import com.cloudcostai.entity.CostAnomaly;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CostAnomalyRepository extends JpaRepository<CostAnomaly, Long> {

    List<CostAnomaly> findByOrganizationIdOrderByDetectedAtDesc(Long organizationId);

    List<CostAnomaly> findByOrganizationIdAndSeverityOrderByDetectedAtDesc(Long organizationId, AnomalySeverity severity);

    List<CostAnomaly> findByOrganizationIdAndStatusOrderByDetectedAtDesc(Long organizationId, AnomalyStatus status);

    List<CostAnomaly> findByOrganizationIdAndSeverityAndStatusOrderByDetectedAtDesc(Long organizationId, AnomalySeverity severity, AnomalyStatus status);

    Optional<CostAnomaly> findByIdAndOrganizationId(Long id, Long organizationId);

    boolean existsByOrganizationIdAndDimensionKeyAndAnomalyPeriodStartAndStatusIn(
            Long organizationId, String dimensionKey, LocalDate anomalyPeriodStart, Collection<AnomalyStatus> statuses);

    Optional<CostAnomaly> findByOrganizationIdAndDimensionKeyAndAnomalyPeriodStart(
            Long organizationId, String dimensionKey, LocalDate anomalyPeriodStart);

    long countByOrganizationId(Long organizationId);

    long countByOrganizationIdAndSeverity(Long organizationId, AnomalySeverity severity);

    long countByOrganizationIdAndStatus(Long organizationId, AnomalyStatus status);

    @Query("SELECT COALESCE(SUM(ca.excessCost), 0) FROM CostAnomaly ca " +
           "WHERE ca.organization.id = :orgId AND ca.status != com.cloudcostai.entity.AnomalyStatus.DISMISSED AND ca.status != com.cloudcostai.entity.AnomalyStatus.IGNORED")
    BigDecimal sumActiveExcessCostByOrganizationId(@Param("orgId") Long orgId);

    @Query("SELECT ca FROM CostAnomaly ca " +
           "WHERE ca.organization.id = :orgId " +
           "AND (:severity IS NULL OR ca.severity = :severity) " +
           "AND (:status IS NULL OR ca.status = :status) " +
           "AND (:provider IS NULL OR ca.cloudAccount.provider = :provider) " +
           "AND (:serviceName IS NULL OR LOWER(ca.serviceName) LIKE LOWER(CONCAT('%', :serviceName, '%'))) " +
           "AND (:startInstant IS NULL OR ca.detectedAt >= :startInstant) " +
           "AND (:endInstant IS NULL OR ca.detectedAt <= :endInstant) " +
           "ORDER BY ca.detectedAt DESC")
    Page<CostAnomaly> findFilteredAnomalies(
            @Param("orgId") Long orgId,
            @Param("severity") AnomalySeverity severity,
            @Param("status") AnomalyStatus status,
            @Param("provider") CloudProvider provider,
            @Param("serviceName") String serviceName,
            @Param("startInstant") Instant startInstant,
            @Param("endInstant") Instant endInstant,
            Pageable pageable);

    @Query("SELECT ca FROM CostAnomaly ca " +
           "WHERE ca.organization.id = :orgId " +
           "AND (:severity IS NULL OR ca.severity = :severity) " +
           "AND (:status IS NULL OR ca.status = :status) " +
           "AND (:provider IS NULL OR ca.cloudAccount.provider = :provider) " +
           "AND (:serviceName IS NULL OR LOWER(ca.serviceName) LIKE LOWER(CONCAT('%', :serviceName, '%'))) " +
           "AND (:startInstant IS NULL OR ca.detectedAt >= :startInstant) " +
           "AND (:endInstant IS NULL OR ca.detectedAt <= :endInstant) " +
           "ORDER BY ca.detectedAt DESC")
    List<CostAnomaly> findFilteredAnomaliesList(
            @Param("orgId") Long orgId,
            @Param("severity") AnomalySeverity severity,
            @Param("status") AnomalyStatus status,
            @Param("provider") CloudProvider provider,
            @Param("serviceName") String serviceName,
            @Param("startInstant") Instant startInstant,
            @Param("endInstant") Instant endInstant);

    List<CostAnomaly> findByOrganizationIdAndDetectedAtBetweenOrderByDetectedAtAsc(
            Long organizationId, Instant startInstant, Instant endInstant);
}
