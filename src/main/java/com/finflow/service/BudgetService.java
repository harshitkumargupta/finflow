package com.finflow.service;

import com.finflow.dto.BudgetRequest;
import com.finflow.dto.BudgetResponse;
import com.finflow.entity.Budget;
import com.finflow.entity.User;
import com.finflow.exception.ResourceNotFoundException;
import com.finflow.repository.BudgetRepository;
import com.finflow.repository.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BudgetService {
    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final CurrentUserService currentUserService;

    public BudgetService(BudgetRepository budgetRepository,
                         TransactionRepository transactionRepository,
                         CurrentUserService currentUserService) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public BudgetResponse create(BudgetRequest request) {
        User user = currentUserService.get();
        validateDates(request);
        Budget budget = new Budget();
        budget.setUser(user);
        apply(budget, request);
        budget.setCreatedAt(LocalDateTime.now());
        budget.setUpdatedAt(LocalDateTime.now());
        return toResponse(budgetRepository.save(budget));
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> list() {
        User user = currentUserService.get();
        return budgetRepository.findByUserIdOrderByStartDateDesc(user.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BudgetResponse get(Long id) {
        return toResponse(findOwned(id));
    }

    @Transactional
    public BudgetResponse update(Long id, BudgetRequest request) {
        validateDates(request);
        Budget budget = findOwned(id);
        apply(budget, request);
        budget.setUpdatedAt(LocalDateTime.now());
        return toResponse(budgetRepository.save(budget));
    }

    @Transactional
    public void delete(Long id) {
        budgetRepository.delete(findOwned(id));
    }

    private Budget findOwned(Long id) {
        User user = currentUserService.get();
        return budgetRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
    }

    private void validateDates(BudgetRequest request) {
        if (request.getBudgetAmount() == null || request.getBudgetAmount().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Budget amount must be greater than zero");
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Budget dates are required");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Budget end date must not be before its start date");
        }
    }

    private void apply(Budget budget, BudgetRequest request) {
        budget.setCategory(request.getCategory());
        budget.setBudgetAmount(request.getBudgetAmount());
        budget.setStartDate(request.getStartDate());
        budget.setEndDate(request.getEndDate());
        LocalDate today = LocalDate.now();
        budget.setStatus(today.isBefore(request.getStartDate()) ? "UPCOMING"
                : today.isAfter(request.getEndDate()) ? "ENDED" : "ACTIVE");
    }

    private BudgetResponse toResponse(Budget budget) {
        BigDecimal spent = transactionRepository.sumExpensesForBudget(
                budget.getUser().getId(),
                budget.getCategory(),
                budget.getStartDate().atStartOfDay(),
                budget.getEndDate().plusDays(1).atStartOfDay().minusNanos(1));
        if (spent == null) spent = BigDecimal.ZERO;
        BigDecimal remaining = budget.getBudgetAmount().subtract(spent).max(BigDecimal.ZERO);
        BigDecimal percentage = spent.multiply(BigDecimal.valueOf(100))
                .divide(budget.getBudgetAmount(), 2, RoundingMode.HALF_UP);
        String periodStatus = LocalDate.now().isBefore(budget.getStartDate()) ? "UPCOMING"
                : LocalDate.now().isAfter(budget.getEndDate()) ? "ENDED" : "ACTIVE";
        BudgetProjectionCalculator.Result projection = BudgetProjectionCalculator.calculate(
                budget.getBudgetAmount(), spent, budget.getStartDate(), budget.getEndDate(), LocalDate.now());
        return new BudgetResponse(
                budget.getId(), budget.getCategory(), budget.getBudgetAmount(),
                spent, remaining, percentage, spent.compareTo(budget.getBudgetAmount()) >= 0,
                budget.getStartDate(), budget.getEndDate(),
                periodStatus, projection.projectedMonthEnd(), projection.status(), projection.percentUsed());
    }
}
