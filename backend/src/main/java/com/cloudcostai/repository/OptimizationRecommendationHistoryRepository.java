package com.cloudcostai.repository;

import com.cloudcostai.entity.OptimizationRecommendationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OptimizationRecommendationHistoryRepository extends JpaRepository<OptimizationRecommendationHistory, Long> {

    List<OptimizationRecommendationHistory> findByRecommendationIdOrderByCreatedAtDesc(Long recommendationId);
}

