package com.cloudcostai.repository;

import com.cloudcostai.entity.BillingRecord;
import com.cloudcostai.repository.projection.MonthlyTrendSummary;
import com.cloudcostai.repository.projection.ProviderCostSummary;
import com.cloudcostai.repository.projection.ServiceCostSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillingRecordRepository extends JpaRepository<BillingRecord, Long>, JpaSpecificationExecutor<BillingRecord> {

    Optional<BillingRecord> findByIdAndOrganizationId(Long id, Long organizationId);

    Page<BillingRecord> findByOrganizationId(Long organizationId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(b.costAmount), 0) FROM BillingRecord b " +
           "WHERE b.organization.id = :orgId " +
           "AND (:startDate IS NULL OR b.billingPeriodStart >= :startDate) " +
           "AND (:endDate IS NULL OR b.billingPeriodEnd <= :endDate)")
    BigDecimal calculateTotalCost(
            @Param("orgId") Long orgId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(b) FROM BillingRecord b " +
           "WHERE b.organization.id = :orgId " +
           "AND (:startDate IS NULL OR b.billingPeriodStart >= :startDate) " +
           "AND (:endDate IS NULL OR b.billingPeriodEnd <= :endDate)")
    Long countRecords(
            @Param("orgId") Long orgId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT ca.provider AS provider, SUM(b.costAmount) AS totalCost, COUNT(b) AS recordCount " +
           "FROM BillingRecord b JOIN b.cloudAccount ca " +
           "WHERE b.organization.id = :orgId " +
           "AND (:startDate IS NULL OR b.billingPeriodStart >= :startDate) " +
           "AND (:endDate IS NULL OR b.billingPeriodEnd <= :endDate) " +
           "GROUP BY ca.provider ORDER BY SUM(b.costAmount) DESC")
    List<ProviderCostSummary> aggregateCostByProvider(
            @Param("orgId") Long orgId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT b.serviceName AS serviceName, ca.provider AS provider, SUM(b.costAmount) AS totalCost, COUNT(b) AS recordCount " +
           "FROM BillingRecord b JOIN b.cloudAccount ca " +
           "WHERE b.organization.id = :orgId " +
           "AND (:startDate IS NULL OR b.billingPeriodStart >= :startDate) " +
           "AND (:endDate IS NULL OR b.billingPeriodEnd <= :endDate) " +
           "GROUP BY b.serviceName, ca.provider ORDER BY SUM(b.costAmount) DESC")
    List<ServiceCostSummary> aggregateCostByService(
            @Param("orgId") Long orgId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT YEAR(b.billingPeriodStart) AS year, MONTH(b.billingPeriodStart) AS month, ca.provider AS provider, SUM(b.costAmount) AS totalCost " +
           "FROM BillingRecord b JOIN b.cloudAccount ca " +
           "WHERE b.organization.id = :orgId " +
           "AND (:startDate IS NULL OR b.billingPeriodStart >= :startDate) " +
           "AND (:endDate IS NULL OR b.billingPeriodEnd <= :endDate) " +
           "GROUP BY YEAR(b.billingPeriodStart), MONTH(b.billingPeriodStart), ca.provider " +
           "ORDER BY YEAR(b.billingPeriodStart) ASC, MONTH(b.billingPeriodStart) ASC")
    List<MonthlyTrendSummary> aggregateMonthlyTrends(
            @Param("orgId") Long orgId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    boolean existsByOrganizationIdAndCloudAccountIdAndServiceNameAndBillingPeriodStartAndBillingPeriodEndAndCostAmount(
            Long orgId, Long cloudAccountId, String serviceName, LocalDate start, LocalDate end, BigDecimal costAmount);
}
