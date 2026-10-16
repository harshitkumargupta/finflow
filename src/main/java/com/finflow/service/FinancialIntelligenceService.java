package com.finflow.service;

import com.finflow.dto.*;
import com.finflow.entity.AutopayAuthorization;
import com.finflow.entity.Transaction;
import com.finflow.entity.User;
import com.finflow.repository.AccountRepository;
import com.finflow.repository.AutopayAuthorizationRepository;
import com.finflow.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FinancialIntelligenceService {
    private static final Set<String> INCOME = Set.of("DEPOSIT", "TRANSFER_IN");
    private static final Set<String> EXPENSE = Set.of("WITHDRAWAL", "TRANSFER_OUT");
    private final CurrentUserService currentUser;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final SafeToSpendService safeToSpendService;
    private final BillService billService;
    private final BudgetService budgetService;
    private final SavingsGoalService goalService;
    private final AutopayAuthorizationRepository autopayRepository;

    public FinancialIntelligenceService(CurrentUserService currentUser, AccountRepository accountRepository,
                                        TransactionRepository transactionRepository, SafeToSpendService safeToSpendService,
                                        BillService billService, BudgetService budgetService,
                                        SavingsGoalService goalService, AutopayAuthorizationRepository autopayRepository) {
        this.currentUser = currentUser; this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository; this.safeToSpendService = safeToSpendService;
        this.billService = billService; this.budgetService = budgetService; this.goalService = goalService;
        this.autopayRepository = autopayRepository;
    }

    @Transactional(readOnly = true)
    public FinancialSummaryResponse summary() {
        User user = currentUser.get();
        List<Transaction> transactions = transactionRepository.findSuccessfulByUserIdOrderByCreatedAtDesc(user.getId());
        SafeToSpendResponse safe = safeToSpendService.calculateForCurrentUser();
        LocalDate today = LocalDate.now(); LocalDate monthStart = today.withDayOfMonth(1);
        BigDecimal income = sum(transactions, INCOME, monthStart.atStartOfDay(), LocalDateTime.now());
        BigDecimal expenses = sum(transactions, EXPENSE, monthStart.atStartOfDay(), LocalDateTime.now());
        BigDecimal goalCommitments = autopayRepository.findByUserIdOrderByNextPaymentDateAsc(user.getId()).stream()
                .filter(a -> "ACTIVE".equalsIgnoreCase(a.getStatus()) && a.getSavingsGoal() != null)
                .map(AutopayAuthorization::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal autopay = autopayRepository.findByUserIdOrderByNextPaymentDateAsc(user.getId()).stream()
                .filter(a -> "ACTIVE".equalsIgnoreCase(a.getStatus())).map(AutopayAuthorization::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal budgetUsage = budgetService.list().stream().filter(b -> "ACTIVE".equals(b.periodStatus()))
                .map(BudgetResponse::percentUsed).max(Comparator.naturalOrder()).orElse(null);
        CashFlowAnalysis cashFlow = cashFlow(transactions, today);
        List<FinancialInsight> insights = insights(transactions, safe, cashFlow, budgetUsage, goalCommitments);
        return new FinancialSummaryResponse(accountRepository.sumBalancesByUserId(user.getId()), safe.safeToSpend(), income,
                expenses, income.subtract(expenses), billService.dueWithinDays(30).stream().filter(b -> !b.nextDueDate().isBefore(today)).map(BillResponse::amount).reduce(BigDecimal.ZERO, BigDecimal::add),
                autopay, goalCommitments, budgetUsage, income.signum() == 0 ? null : income.subtract(expenses).multiply(BigDecimal.valueOf(100)).divide(income, 2, RoundingMode.HALF_UP),
                cashFlow, recurring(transactions), insights, goalFeasibility(transactions));
    }

    @Transactional(readOnly = true)
    public PurchasePlanResponse purchasePlan(PurchasePlanRequest request) {
        FinancialSummaryResponse snapshot = summary();
        BigDecimal before = snapshot.safeToSpend(); BigDecimal after = before.subtract(request.amount());
        BigDecimal budgetImpact = null;
        if (request.category() != null) budgetImpact = budgetService.list().stream()
                .filter(b -> request.category().equalsIgnoreCase(b.category()) && "ACTIVE".equals(b.periodStatus()))
                .map(BudgetResponse::remainingAmount).findFirst().map(v -> v.subtract(request.amount())).orElse(null);
        BigDecimal goalImpact = snapshot.goalCommitments().signum() == 0 ? null : snapshot.goalCommitments().add(request.amount());
        String recommendation = recommendPurchase(before, after, budgetImpact);
        String explanation = recommendation.equals("SAFE") ? "This purchase stays within the current planning buffer."
                : recommendation.equals("CAUTION") ? "This purchase is possible but materially reduces your planning buffer or category budget."
                : "This purchase would make safe-to-spend negative or compromise an active budget.";
        return new PurchasePlanResponse(snapshot.currentBalance(), before, request.amount(), after, goalImpact, budgetImpact, recommendation, explanation);
    }

    public static String recommendPurchase(BigDecimal before, BigDecimal after, BigDecimal budgetAfter) {
        if (after.signum() < 0) return "NOT_RECOMMENDED";
        if (after.compareTo(before.multiply(BigDecimal.valueOf(.25))) < 0
                || (budgetAfter != null && budgetAfter.signum() < 0)) return "CAUTION";
        return "SAFE";
    }

    private CashFlowAnalysis cashFlow(List<Transaction> tx, LocalDate today) {
        Map<YearMonth, BigDecimal[]> months = new TreeMap<>();
        for (Transaction t : tx) { YearMonth ym = YearMonth.from(t.getCreatedAt()); BigDecimal[] v = months.computeIfAbsent(ym, x -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO}); if (INCOME.contains(t.getType())) v[0] = v[0].add(t.getAmount()); if (EXPENSE.contains(t.getType())) v[1] = v[1].add(t.getAmount()); }
        YearMonth current = YearMonth.from(today); BigDecimal[] c = months.getOrDefault(current, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        if (months.isEmpty()) return new CashFlowAnalysis(null, null, c[0], c[1], null, null, null, false);
        BigDecimal avgIn = months.values().stream().map(v -> v[0]).reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(months.size()), 2, RoundingMode.HALF_UP);
        BigDecimal avgOut = months.values().stream().map(v -> v[1]).reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(months.size()), 2, RoundingMode.HALF_UP);
        BigDecimal change = null; BigDecimal[] previous = months.get(current.minusMonths(1)); if (previous != null && previous[1].signum() > 0) change = c[1].subtract(previous[1]).multiply(BigDecimal.valueOf(100)).divide(previous[1], 2, RoundingMode.HALF_UP);
        String highest = tx.stream().filter(t -> EXPENSE.contains(t.getType()) && t.getCategory() != null).collect(Collectors.groupingBy(Transaction::getCategory, Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add))).entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
        return new CashFlowAnalysis(avgIn, avgOut, c[0], c[1], avgIn.signum() == 0 ? null : avgIn.subtract(avgOut).multiply(BigDecimal.valueOf(100)).divide(avgIn, 2, RoundingMode.HALF_UP), highest, change, months.size() >= 2);
    }

    private List<RecurringExpense> recurring(List<Transaction> tx) {
        Map<String, List<Transaction>> groups = tx.stream().filter(t -> EXPENSE.contains(t.getType()) && t.getAmount() != null).collect(Collectors.groupingBy(t -> (t.getDescription() == null ? "UNKNOWN" : t.getDescription().trim().toLowerCase()) + "|" + t.getCategory() + "|" + t.getAmount().setScale(2, RoundingMode.HALF_UP)));
        return groups.values().stream().filter(v -> v.size() >= 3).map(v -> { v.sort(Comparator.comparing(Transaction::getCreatedAt)); long span = ChronoUnit.DAYS.between(v.get(0).getCreatedAt().toLocalDate(), v.get(v.size()-1).getCreatedAt().toLocalDate()); long interval = span / Math.max(1, v.size()-1); String frequency = interval >= 5 && interval <= 10 ? "WEEKLY" : interval >= 25 && interval <= 35 ? "MONTHLY" : interval >= 80 && interval <= 100 ? "QUARTERLY" : "IRREGULAR"; BigDecimal confidence = frequency.equals("IRREGULAR") ? BigDecimal.valueOf(55) : BigDecimal.valueOf(80); return new RecurringExpense(v.get(0).getDescription(), v.get(0).getCategory(), v.get(0).getAmount(), frequency, v.size(), confidence, v.get(v.size()-1).getCreatedAt().toLocalDate(), "Same merchant/category and amount repeated at least three times."); }).filter(r -> !"IRREGULAR".equals(r.frequency())).toList();
    }

    private List<GoalFeasibility> goalFeasibility(List<Transaction> tx) {
        Long userId = currentUser.get().getId();
        return goalService.list().stream().map(g -> { BigDecimal remaining = g.remainingAmount(); BigDecimal monthly = autopayRepository.findBySavingsGoalIdAndUserIdOrderByNextPaymentDateAsc(g.id(), userId).stream().filter(a -> "ACTIVE".equalsIgnoreCase(a.getStatus())).map(AutopayAuthorization::getAmount).findFirst().orElse(null); boolean feasible = remaining.signum() == 0 || g.targetDate() == null || (monthly != null && g.requiredMonthlySavings() != null && monthly.compareTo(g.requiredMonthlySavings()) >= 0); LocalDate projected = feasible && monthly != null && monthly.signum() > 0 && remaining.signum() > 0 ? LocalDate.now().plusMonths(remaining.divide(monthly, 0, RoundingMode.CEILING).longValue()) : feasible ? g.targetDate() : null; return new GoalFeasibility(g.id(), g.name(), g.targetAmount(), g.currentAmount(), remaining, g.targetDate(), g.requiredMonthlySavings(), monthly, projected, feasible, feasible ? "Current plan supports the target date." : "No contribution plan currently supports the target date."); }).toList();
    }

    private List<FinancialInsight> insights(List<Transaction> tx, SafeToSpendResponse safe, CashFlowAnalysis cash, BigDecimal budgetUsage, BigDecimal goals) {
        List<FinancialInsight> result = new ArrayList<>(); if (cash.monthOverMonthExpenseChange() != null && cash.monthOverMonthExpenseChange().compareTo(BigDecimal.ZERO) > 0) result.add(new FinancialInsight("SPENDING_INCREASE", "Spending increased", "Current month expenses are higher than the previous month.", "WARNING", cash.monthOverMonthExpenseChange())); if (budgetUsage != null && budgetUsage.compareTo(BigDecimal.valueOf(100)) >= 0) result.add(new FinancialInsight("BUDGET_EXCEEDED", "Budget exceeded", "At least one active budget has reached its limit.", "IMPORTANT", budgetUsage)); if (safe.safeToSpend().signum() < 0) result.add(new FinancialInsight("SAFE_TO_SPEND_LOW", "Planning buffer is negative", "Upcoming commitments exceed the current available balance.", "IMPORTANT", safe.safeToSpend())); return result.stream().limit(5).toList();
    }

    private BigDecimal sum(List<Transaction> tx, Set<String> types, LocalDateTime start, LocalDateTime end) { return tx.stream().filter(t -> types.contains(t.getType()) && !t.getCreatedAt().isBefore(start) && !t.getCreatedAt().isAfter(end)).map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add); }
}
