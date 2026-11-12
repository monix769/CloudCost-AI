package com.cloudcostai.repository;

import com.cloudcostai.entity.AnomalyDetectionConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnomalyDetectionConfigRepository extends JpaRepository<AnomalyDetectionConfig, Long> {

    Optional<AnomalyDetectionConfig> findByOrganizationId(Long organizationId);
}

