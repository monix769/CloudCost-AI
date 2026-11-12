package com.cloudcostai.repository;

import com.cloudcostai.entity.ResourceUtilizationSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceUtilizationSnapshotRepository extends JpaRepository<ResourceUtilizationSnapshot, Long> {

    List<ResourceUtilizationSnapshot> findByOrganizationIdOrderBySnapshotDateDesc(Long organizationId, Pageable pageable);

    Page<ResourceUtilizationSnapshot> findByOrganizationId(Long organizationId, Pageable pageable);

    Optional<ResourceUtilizationSnapshot> findTopByOrganizationIdAndResourceIdOrderBySnapshotDateDesc(Long organizationId, String resourceId);

    List<ResourceUtilizationSnapshot> findByOrganizationIdAndResourceIdIn(Long organizationId, Collection<String> resourceIds);

    List<ResourceUtilizationSnapshot> findByOrganizationIdAndCloudAccountId(Long organizationId, Long cloudAccountId);

    long countByOrganizationId(Long organizationId);
}

