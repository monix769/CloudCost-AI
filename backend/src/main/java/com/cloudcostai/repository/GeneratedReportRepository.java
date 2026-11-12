package com.cloudcostai.repository;

import com.cloudcostai.entity.GeneratedReport;
import com.cloudcostai.entity.ReportFormat;
import com.cloudcostai.entity.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface GeneratedReportRepository extends JpaRepository<GeneratedReport, Long> {

    Optional<GeneratedReport> findByIdAndOrganizationId(Long id, Long organizationId);

    Page<GeneratedReport> findByOrganizationId(Long organizationId, Pageable pageable);

    Page<GeneratedReport> findByOrganizationIdAndReportType(Long organizationId, ReportType reportType, Pageable pageable);

    @Query("SELECT r FROM GeneratedReport r " +
           "WHERE r.organization.id = :orgId " +
           "AND (:reportType IS NULL OR r.reportType = :reportType) " +
           "AND (:fileFormat IS NULL OR r.fileFormat = :fileFormat) " +
           "AND (:startDate IS NULL OR r.createdAt >= :startDate) " +
           "ORDER BY r.createdAt DESC")
    Page<GeneratedReport> findFilteredReports(
            @Param("orgId") Long orgId,
            @Param("reportType") ReportType reportType,
            @Param("fileFormat") ReportFormat fileFormat,
            @Param("startDate") java.time.Instant startDate,
            Pageable pageable);

    long countByOrganizationId(Long organizationId);
}

