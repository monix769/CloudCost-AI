package com.cloudcostai.service;

import com.cloudcostai.dto.BudgetRequest;
import com.cloudcostai.dto.BudgetResponse;
import com.cloudcostai.entity.Budget;
import com.cloudcostai.entity.BudgetPeriod;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.exception.DuplicateResourceException;
import com.cloudcostai.exception.ResourceNotFoundException;
import com.cloudcostai.mapper.BudgetMapper;
import com.cloudcostai.repository.BillingRecordRepository;
import com.cloudcostai.repository.BudgetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final BillingRecordRepository billingRecordRepository;
    private final BudgetMapper budgetMapper;
    private final OrganizationService organizationService;

    @Transactional(readOnly = true)
    public List<BudgetResponse> getAllBudgets() {
        Organization org = organizationService.getCurrentUserOrganization();
        List<Budget> budgets = budgetRepository.findByOrganizationId(org.getId());

        return budgets.stream()
                .map(budget -> {
                    BigDecimal liveSpend = calculateLiveSpendForBudget(budget, org.getId());
                    return budgetMapper.toResponse(budget, liveSpend);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public BudgetResponse getBudgetById(Long id) {
        Budget budget = findBudgetEntity(id);
        BigDecimal liveSpend = calculateLiveSpendForBudget(budget, budget.getOrganization().getId());
        return budgetMapper.toResponse(budget, liveSpend);
    }

    @Transactional
    public BudgetResponse createBudget(BudgetRequest request) {
        Organization org = organizationService.getCurrentUserOrganization();

        if (budgetRepository.existsByOrganizationIdAndNameIgnoreCase(org.getId(), request.getName().trim())) {
            throw new DuplicateResourceException("Budget", "name", request.getName());
        }

        Budget budget = budgetMapper.toEntity(request, org);
        Budget saved = budgetRepository.save(budget);
        log.info("Created budget [ID: {}, Name: {}, Limit: {}]", saved.getId(), saved.getName(), saved.getAmountLimit());

        BigDecimal liveSpend = calculateLiveSpendForBudget(saved, org.getId());
        return budgetMapper.toResponse(saved, liveSpend);
    }

    @Transactional
    public BudgetResponse updateBudget(Long id, BudgetRequest request) {
        Budget budget = findBudgetEntity(id);
        Organization org = budget.getOrganization();

        if (budgetRepository.existsByOrganizationIdAndNameIgnoreCaseAndIdNot(org.getId(), request.getName().trim(), id)) {
            throw new DuplicateResourceException("Budget", "name", request.getName());
        }

        budgetMapper.updateEntity(budget, request);
        Budget updated = budgetRepository.save(budget);
        log.info("Updated budget [ID: {}, Name: {}]", updated.getId(), updated.getName());

        BigDecimal liveSpend = calculateLiveSpendForBudget(updated, org.getId());
        return budgetMapper.toResponse(updated, liveSpend);
    }

    @Transactional
    public void deleteBudget(Long id) {
        Budget budget = findBudgetEntity(id);
        budgetRepository.delete(budget);
        log.info("Deleted budget [ID: {}]", id);
    }

    @Transactional(readOnly = true)
    public Budget findBudgetEntity(Long id) {
        Organization org = organizationService.getCurrentUserOrganization();
        return budgetRepository.findByIdAndOrganizationId(id, org.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Budget", "id", id));
    }

    private BigDecimal calculateLiveSpendForBudget(Budget budget, Long orgId) {
        LocalDate now = LocalDate.now();
        LocalDate startDate;
        LocalDate endDate;

        if (budget.getPeriod() == BudgetPeriod.MONTHLY) {
            startDate = now.withDayOfMonth(1);
            endDate = now.with(TemporalAdjusters.lastDayOfMonth());
        } else if (budget.getPeriod() == BudgetPeriod.QUARTERLY) {
            int currentQuarterMonth = ((now.getMonthValue() - 1) / 3) * 3 + 1;
            startDate = LocalDate.of(now.getYear(), currentQuarterMonth, 1);
            endDate = startDate.plusMonths(3).minusDays(1);
        } else { // ANNUAL
            startDate = LocalDate.of(now.getYear(), 1, 1);
            endDate = LocalDate.of(now.getYear(), 12, 31);
        }

        BigDecimal calculatedSpend = billingRecordRepository.calculateTotalCost(orgId, startDate, endDate);

        // If no billing entries exist for the strict calendar window, calculate total available spend
        if (calculatedSpend == null || calculatedSpend.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal totalHistorical = billingRecordRepository.calculateTotalCost(orgId, null, null);
            if (totalHistorical != null && totalHistorical.compareTo(BigDecimal.ZERO) > 0) {
                calculatedSpend = totalHistorical;
            } else if (budget.getCurrentSpend() != null && budget.getCurrentSpend().compareTo(BigDecimal.ZERO) > 0) {
                calculatedSpend = budget.getCurrentSpend();
            } else {
                calculatedSpend = BigDecimal.ZERO;
            }
        }

        return calculatedSpend.setScale(2, RoundingMode.HALF_UP);
    }
}
