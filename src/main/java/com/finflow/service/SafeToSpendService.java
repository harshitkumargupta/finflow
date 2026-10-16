package com.finflow.service;

import com.finflow.dto.BillResponse;
import com.finflow.dto.BudgetResponse;
import com.finflow.dto.SafeToSpendBreakdown;
import com.finflow.dto.SafeToSpendResponse;
import com.finflow.entity.AutopayAuthorization;
import com.finflow.entity.IncomeSchedule;
import com.finflow.repository.IncomeScheduleRepository;
import com.finflow.entity.User;
import com.finflow.repository.AccountRepository;
import com.finflow.repository.AutopayAuthorizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class SafeToSpendService {
    private static final int HORIZON_DAYS = IncomeScheduleService.HORIZON_DAYS;
    public static final String EXPECTED_INCOME = "EXPECTED_INCOME";

    private final AccountRepository accountRepository;
    private final AutopayAuthorizationRepository autopayRepository;
    private final CurrentUserService currentUserService;
    private final BillService billService;
    private final BudgetService budgetService;
    private final IncomeScheduleRepository incomeScheduleRepository;

    public SafeToSpendService(AccountRepository accountRepository,
                              AutopayAuthorizationRepository autopayRepository,
                              CurrentUserService currentUserService,
                              BillService billService,
                              BudgetService budgetService,
                              IncomeScheduleRepository incomeScheduleRepository) {
        this.accountRepository = accountRepository;
        this.autopayRepository = autopayRepository;
        this.currentUserService = currentUserService;
        this.billService = billService;
        this.budgetService = budgetService;
        this.incomeScheduleRepository = incomeScheduleRepository;
    }

    @Transactional(readOnly = true)
    public SafeToSpendResponse calculateForCurrentUser() {
        User user = currentUserService.get();
        LocalDate today = LocalDate.now();
        LocalDate horizon = today.plusDays(HORIZON_DAYS);
        BigDecimal currentBalance = accountRepository.sumBalancesByUserId(user.getId());
        if (currentBalance == null) currentBalance = BigDecimal.ZERO;

        return calculate(currentBalance, billService.dueWithinDays(HORIZON_DAYS),
                autopayRepository.findByUserIdOrderByNextPaymentDateAsc(user.getId()), budgetService.list(),
                incomeScheduleRepository.findByUserIdAndActiveTrueOrderByNextExpectedDateAsc(user.getId()), today);
    }

    public SafeToSpendResponse calculate(BigDecimal currentBalance, List<BillResponse> bills,
                                         List<AutopayAuthorization> authorizations,
                                         List<BudgetResponse> budgets, LocalDate today) {
        return calculate(currentBalance, bills, authorizations, budgets, List.of(), today);
    }

    public SafeToSpendResponse calculate(BigDecimal currentBalance, List<BillResponse> bills,
                                         List<AutopayAuthorization> authorizations,
                                         List<BudgetResponse> budgets, List<IncomeSchedule> incomes,
                                         LocalDate today) {
        LocalDate horizon = today.plusDays(HORIZON_DAYS);
        BigDecimal balance = currentBalance == null ? BigDecimal.ZERO : currentBalance;
        List<SafeToSpendBreakdown> breakdown = new ArrayList<>();
        BigDecimal deductions = BigDecimal.ZERO;

        for (BillResponse bill : bills) {
            if (!bill.active() || bill.nextDueDate() == null
                    || bill.nextDueDate().isBefore(today) || bill.nextDueDate().isAfter(horizon)) continue;
            deductions = add(breakdown, deductions,
                    new SafeToSpendBreakdown("UPCOMING_BILL", bill.name(), money(bill.amount())));
        }

        for (AutopayAuthorization authorization : authorizations) {
            if (!"ACTIVE".equalsIgnoreCase(authorization.getStatus())
                    || authorization.getNextPaymentDate() == null
                    || authorization.getNextPaymentDate().isBefore(today)
                    || authorization.getNextPaymentDate().isAfter(horizon)) continue;
            String type = authorization.getSavingsGoal() == null ? "AUTOPAY" : "GOAL_CONTRIBUTION";
            deductions = add(breakdown, deductions,
                    new SafeToSpendBreakdown(type, authorization.getMerchantName(), money(authorization.getAmount())));
        }

        boolean hasOverall = budgets.stream().anyMatch(b -> "OVERALL".equals(b.category())
                && "ACTIVE".equals(b.periodStatus()));
        for (BudgetResponse budget : budgets) {
            if (!"ACTIVE".equals(budget.periodStatus())) continue;
            if (hasOverall && !"OVERALL".equals(budget.category())) continue;
            BigDecimal reserve = budget.remainingAmount().max(BigDecimal.ZERO);
            if (reserve.signum() == 0) continue;
            deductions = add(breakdown, deductions,
                    new SafeToSpendBreakdown("BUDGET_RESERVE", budget.category(), reserve));
        }

        for (IncomeSchedule income : incomes) {
            List<LocalDate> dates = IncomeScheduleService.occurrencesWithin(income, today, horizon);
            if (dates.isEmpty()) continue;
            BigDecimal expected = money(income.getAmount()).multiply(BigDecimal.valueOf(dates.size()));
            breakdown.add(new SafeToSpendBreakdown(EXPECTED_INCOME, income.getSourceName(), expected));
            deductions = deductions.subtract(expected);
        }

        BigDecimal safe = balance.subtract(deductions);
        return new SafeToSpendResponse(balance, safe,
                safe.divide(BigDecimal.valueOf(HORIZON_DAYS), 2, RoundingMode.HALF_UP),
                horizon, List.copyOf(breakdown));
    }

    private BigDecimal add(List<SafeToSpendBreakdown> breakdown, BigDecimal total,
                           SafeToSpendBreakdown item) {
        breakdown.add(item);
        return total.add(item.amount());
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.max(BigDecimal.ZERO);
    }
}
