package com.cloudcostai.repository;

import com.cloudcostai.entity.AuditActionType;
import com.cloudcostai.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Optional<AuditLog> findByIdAndOrganizationId(Long id, Long organizationId);

    @Query("SELECT a FROM AuditLog a " +
           "WHERE a.organization.id = :orgId " +
           "AND (:actionType IS NULL OR a.actionType = :actionType) " +
           "AND (:userEmail IS NULL OR LOWER(a.userEmail) LIKE LOWER(CONCAT('%', :userEmail, '%'))) " +
           "AND (:resourceType IS NULL OR a.resourceType = :resourceType) " +
           "AND (:startInstant IS NULL OR a.createdAt >= :startInstant) " +
           "AND (:endInstant IS NULL OR a.createdAt <= :endInstant) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> findFilteredAuditLogs(
            @Param("orgId") Long orgId,
            @Param("actionType") AuditActionType actionType,
            @Param("userEmail") String userEmail,
            @Param("resourceType") String resourceType,
            @Param("startInstant") Instant startInstant,
            @Param("endInstant") Instant endInstant,
            Pageable pageable);

    long countByOrganizationId(Long organizationId);
}

