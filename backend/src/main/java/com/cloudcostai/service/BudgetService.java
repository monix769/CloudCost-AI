package com.cloudcostai.service;

import com.cloudcostai.dto.BudgetItemUtilizationResponse;
import com.cloudcostai.dto.BudgetHealthStatus;
import com.cloudcostai.dto.BudgetRequest;
import com.cloudcostai.dto.BudgetResponse;
import com.cloudcostai.dto.BudgetUtilizationResponse;
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
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
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

    @Transactional(readOnly = true)
    public BudgetUtilizationResponse getBudgetUtilizationAnalytics(String requestedCurrency) {
        Organization org = organizationService.getCurrentUserOrganization();
        Long orgId = org.getId();
        List<Budget> budgets = budgetRepository.findByOrganizationId(orgId);

        String activeCurrency = (requestedCurrency != null && !requestedCurrency.trim().isEmpty())
                ? requestedCurrency.trim().toUpperCase()
                : (budgets.isEmpty() ? "USD" : budgets.get(0).getCurrency());

        BigDecimal totalAllocated = BigDecimal.ZERO;
        BigDecimal totalCurrent = BigDecimal.ZERO;
        BigDecimal totalProjected = BigDecimal.ZERO;
        List<BudgetItemUtilizationResponse> items = new ArrayList<>();

        LocalDate now = LocalDate.now();

        for (Budget budget : budgets) {
            BigDecimal limit = budget.getAmountLimit();
            BigDecimal spend = calculateLiveSpendForBudget(budget, orgId);

            totalAllocated = totalAllocated.add(limit);
            totalCurrent = totalCurrent.add(spend);

            BigDecimal remaining = limit.subtract(spend);
            BigDecimal utilization = BigDecimal.ZERO;
            if (limit.compareTo(BigDecimal.ZERO) > 0) {
                utilization = spend.multiply(BigDecimal.valueOf(100)).divide(limit, 2, RoundingMode.HALF_UP);
            }

            int daysElapsed = Math.max(1, now.getDayOfMonth());
            int totalDaysInPeriod = now.lengthOfMonth();
            if (budget.getPeriod() == BudgetPeriod.QUARTERLY) {
                totalDaysInPeriod = 90;
                int currentQuarterMonth = ((now.getMonthValue() - 1) / 3) * 3 + 1;
                LocalDate quarterStart = LocalDate.of(now.getYear(), currentQuarterMonth, 1);
                daysElapsed = Math.max(1, (int) ChronoUnit.DAYS.between(quarterStart, now) + 1);
            } else if (budget.getPeriod() == BudgetPeriod.ANNUAL) {
                totalDaysInPeriod = now.isLeapYear() ? 366 : 365;
                daysElapsed = Math.max(1, now.getDayOfYear());
            }

            BigDecimal burnRate = spend.divide(BigDecimal.valueOf(daysElapsed), 2, RoundingMode.HALF_UP);
            BigDecimal projectedSpend = burnRate.multiply(BigDecimal.valueOf(totalDaysInPeriod)).setScale(2, RoundingMode.HALF_UP);
            totalProjected = totalProjected.add(projectedSpend);

            BudgetHealthStatus health;
            if (utilization.compareTo(BigDecimal.valueOf(100.0)) >= 0) {
                health = BudgetHealthStatus.EXCEEDED;
            } else if (utilization.compareTo(BigDecimal.valueOf(90.0)) >= 0) {
                health = BudgetHealthStatus.CRITICAL;
            } else if (utilization.compareTo(BigDecimal.valueOf(70.0)) >= 0) {
                health = BudgetHealthStatus.WARNING;
            } else {
                health = BudgetHealthStatus.HEALTHY;
            }

            String risk;
            if (projectedSpend.compareTo(limit) > 0 || health == BudgetHealthStatus.EXCEEDED) {
                risk = (utilization.compareTo(BigDecimal.valueOf(100.0)) >= 0) ? "CRITICAL" : "HIGH";
            } else if (utilization.compareTo(BigDecimal.valueOf(70.0)) >= 0) {
                risk = "MEDIUM";
            } else {
                risk = "LOW";
            }

            items.add(BudgetItemUtilizationResponse.builder()
                    .budgetId(budget.getId())
                    .budgetName(budget.getName())
                    .amountLimit(limit.setScale(2, RoundingMode.HALF_UP))
                    .currentSpend(spend.setScale(2, RoundingMode.HALF_UP))
                    .remainingBudget(remaining.setScale(2, RoundingMode.HALF_UP))
                    .utilizationPercentage(utilization)
                    .burnRatePerDay(burnRate)
                    .estimatedEndOfPeriodSpend(projectedSpend)
                    .overspendingRisk(risk)
                    .healthStatus(health)
                    .period(budget.getPeriod())
                    .alertThresholdPercentage(budget.getAlertThresholdPercentage())
                    .build());
        }

        BigDecimal totalRemaining = totalAllocated.subtract(totalCurrent);
        BigDecimal overallUtilization = BigDecimal.ZERO;
        if (totalAllocated.compareTo(BigDecimal.ZERO) > 0) {
            overallUtilization = totalCurrent.multiply(BigDecimal.valueOf(100))
                    .divide(totalAllocated, 2, RoundingMode.HALF_UP);
        }

        BigDecimal projectedOverrun = totalProjected.compareTo(totalAllocated) > 0
                ? totalProjected.subtract(totalAllocated).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BudgetHealthStatus overallHealth;
        if (overallUtilization.compareTo(BigDecimal.valueOf(100.0)) >= 0) {
            overallHealth = BudgetHealthStatus.EXCEEDED;
        } else if (overallUtilization.compareTo(BigDecimal.valueOf(90.0)) >= 0) {
            overallHealth = BudgetHealthStatus.CRITICAL;
        } else if (overallUtilization.compareTo(BigDecimal.valueOf(70.0)) >= 0) {
            overallHealth = BudgetHealthStatus.WARNING;
        } else {
            overallHealth = BudgetHealthStatus.HEALTHY;
        }

        return BudgetUtilizationResponse.builder()
                .totalAllocatedBudget(totalAllocated.setScale(2, RoundingMode.HALF_UP))
                .totalCurrentSpend(totalCurrent.setScale(2, RoundingMode.HALF_UP))
                .totalRemainingBudget(totalRemaining.setScale(2, RoundingMode.HALF_UP))
                .overallUtilizationPercentage(overallUtilization)
                .estimatedEndOfPeriodSpend(totalProjected.setScale(2, RoundingMode.HALF_UP))
                .projectedOverrun(projectedOverrun)
                .overallHealthStatus(overallHealth)
                .currency(activeCurrency)
                .budgetBreakdowns(items)
                .build();
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
