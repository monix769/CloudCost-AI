package com.cloudcostai.repository;

import com.cloudcostai.entity.CloudProvider;
import com.cloudcostai.entity.OptimizationPriority;
import com.cloudcostai.entity.OptimizationRecommendation;
import com.cloudcostai.entity.OptimizationRecommendationType;
import com.cloudcostai.entity.OptimizationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OptimizationRecommendationRepository extends JpaRepository<OptimizationRecommendation, Long> {

    Optional<OptimizationRecommendation> findByIdAndOrganizationId(Long id, Long organizationId);

    List<OptimizationRecommendation> findByOrganizationIdOrderByEstimatedMonthlySavingsDesc(Long organizationId);

    List<OptimizationRecommendation> findByOrganizationIdAndStatusOrderByEstimatedMonthlySavingsDesc(
            Long organizationId, OptimizationStatus status);

    boolean existsByOrganizationIdAndCloudAccountIdAndRecommendationTypeAndResourceIdAndStatusIn(
            Long organizationId,
            Long cloudAccountId,
            OptimizationRecommendationType recommendationType,
            String resourceId,
            Collection<OptimizationStatus> statuses);

    Optional<OptimizationRecommendation> findTopByOrganizationIdAndCloudAccountIdAndRecommendationTypeAndResourceIdAndStatusIn(
            Long organizationId,
            Long cloudAccountId,
            OptimizationRecommendationType recommendationType,
            String resourceId,
            Collection<OptimizationStatus> statuses);

    @Query("SELECT r FROM OptimizationRecommendation r " +
           "WHERE r.organization.id = :orgId " +
           "AND (:provider IS NULL OR r.cloudProvider = :provider) " +
           "AND (:recType IS NULL OR r.recommendationType = :recType) " +
           "AND (:priority IS NULL OR r.priority = :priority) " +
           "AND (:status IS NULL OR r.status = :status) " +
           "AND (:minSavings IS NULL OR r.estimatedMonthlySavings >= :minSavings) " +
           "AND (:search IS NULL OR LOWER(r.resourceId) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(r.resourceType) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR (r.resourceName IS NOT NULL AND LOWER(r.resourceName) LIKE LOWER(CONCAT('%', :search, '%')))) " +
           "ORDER BY r.estimatedMonthlySavings DESC")
    Page<OptimizationRecommendation> findFilteredRecommendations(
            @Param("orgId") Long orgId,
            @Param("provider") CloudProvider provider,
            @Param("recType") OptimizationRecommendationType recType,
            @Param("priority") OptimizationPriority priority,
            @Param("status") OptimizationStatus status,
            @Param("minSavings") BigDecimal minSavings,
            @Param("search") String search,
            Pageable pageable);

    long countByOrganizationId(Long organizationId);

    long countByOrganizationIdAndStatus(Long organizationId, OptimizationStatus status);

    long countByOrganizationIdAndPriority(Long organizationId, OptimizationPriority priority);

    long countByOrganizationIdAndRecommendationType(Long organizationId, OptimizationRecommendationType recommendationType);

    @Query("SELECT COALESCE(SUM(r.estimatedMonthlySavings), 0) FROM OptimizationRecommendation r " +
           "WHERE r.organization.id = :orgId AND r.status IN (com.cloudcostai.entity.OptimizationStatus.NEW, com.cloudcostai.entity.OptimizationStatus.REVIEWED, com.cloudcostai.entity.OptimizationStatus.APPROVED)")
    BigDecimal sumPotentialMonthlySavings(@Param("orgId") Long orgId);

    @Query("SELECT COALESCE(SUM(r.estimatedAnnualSavings), 0) FROM OptimizationRecommendation r " +
           "WHERE r.organization.id = :orgId AND r.status IN (com.cloudcostai.entity.OptimizationStatus.NEW, com.cloudcostai.entity.OptimizationStatus.REVIEWED, com.cloudcostai.entity.OptimizationStatus.APPROVED)")
    BigDecimal sumPotentialAnnualSavings(@Param("orgId") Long orgId);

    @Query("SELECT COALESCE(SUM(r.realizedMonthlySavings), 0) FROM OptimizationRecommendation r " +
           "WHERE r.organization.id = :orgId AND r.status = com.cloudcostai.entity.OptimizationStatus.IMPLEMENTED")
    BigDecimal sumRealizedMonthlySavings(@Param("orgId") Long orgId);

    @Query("SELECT COALESCE(SUM(r.realizedAnnualSavings), 0) FROM OptimizationRecommendation r " +
           "WHERE r.organization.id = :orgId AND r.status = com.cloudcostai.entity.OptimizationStatus.IMPLEMENTED")
    BigDecimal sumRealizedAnnualSavings(@Param("orgId") Long orgId);

    @Query("SELECT r.cloudProvider, COALESCE(SUM(r.estimatedMonthlySavings), 0) " +
           "FROM OptimizationRecommendation r " +
           "WHERE r.organization.id = :orgId AND r.status != com.cloudcostai.entity.OptimizationStatus.REJECTED " +
           "GROUP BY r.cloudProvider")
    List<Object[]> sumMonthlySavingsByProvider(@Param("orgId") Long orgId);

    @Query("SELECT r.recommendationType, COALESCE(SUM(r.estimatedMonthlySavings), 0), COUNT(r) " +
           "FROM OptimizationRecommendation r " +
           "WHERE r.organization.id = :orgId AND r.status != com.cloudcostai.entity.OptimizationStatus.REJECTED " +
           "GROUP BY r.recommendationType")
    List<Object[]> sumMonthlySavingsByType(@Param("orgId") Long orgId);

    @Query("SELECT r.resourceType, COALESCE(SUM(r.estimatedMonthlySavings), 0) " +
           "FROM OptimizationRecommendation r " +
           "WHERE r.organization.id = :orgId AND r.status != com.cloudcostai.entity.OptimizationStatus.REJECTED " +
           "GROUP BY r.resourceType " +
           "ORDER BY SUM(r.estimatedMonthlySavings) DESC")
    List<Object[]> sumMonthlySavingsByResourceType(@Param("orgId") Long orgId);
}

