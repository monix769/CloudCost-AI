package com.cloudcostai.repository;

import com.cloudcostai.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByOrganizationId(Long organizationId);

    Optional<Budget> findByIdAndOrganizationId(Long id, Long organizationId);

    boolean existsByOrganizationIdAndNameIgnoreCase(Long organizationId, String name);

    boolean existsByOrganizationIdAndNameIgnoreCaseAndIdNot(Long organizationId, String name, Long id);
}
