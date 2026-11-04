package com.cloudcostai.repository;

import com.cloudcostai.entity.SyncHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SyncHistoryRepository extends JpaRepository<SyncHistory, Long> {

    Page<SyncHistory> findByOrganizationId(Long organizationId, Pageable pageable);

    Page<SyncHistory> findByOrganizationIdAndCloudAccountId(Long organizationId, Long cloudAccountId, Pageable pageable);

    Optional<SyncHistory> findByIdAndOrganizationId(Long id, Long organizationId);

    List<SyncHistory> findTop10ByOrganizationIdOrderBySyncStartTimeDesc(Long organizationId);

    Optional<SyncHistory> findTopByOrganizationIdAndCloudAccountIdOrderBySyncStartTimeDesc(Long organizationId, Long cloudAccountId);
}
