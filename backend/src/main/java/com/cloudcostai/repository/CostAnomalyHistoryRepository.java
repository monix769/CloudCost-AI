package com.cloudcostai.repository;

import com.cloudcostai.entity.CostAnomalyHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CostAnomalyHistoryRepository extends JpaRepository<CostAnomalyHistory, Long> {

    List<CostAnomalyHistory> findByCostAnomalyIdOrderByCreatedAtDesc(Long costAnomalyId);
}

