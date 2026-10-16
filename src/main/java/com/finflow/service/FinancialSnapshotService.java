package com.finflow.service;

import com.finflow.dto.FinancialSnapshot;
import com.finflow.dto.SpendingBreakdown;
import com.finflow.entity.AutopayAuthorization;
import com.finflow.entity.Bill;
import com.finflow.entity.SavingsGoal;
import com.finflow.entity.Transaction;
import com.finflow.entity.User;
import com.finflow.repository.AccountRepository;
import com.finflow.repository.AutopayAuthorizationRepository;
import com.finflow.repository.BillRepository;
import com.finflow.repository.SavingsGoalRepository;
import com.finflow.repository.TransactionRepository;
import com.finflow.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FinancialSnapshotService {
    private static final Set<String> ESSENTIAL = Set.of("BILLS", "RENT", "MEDICAL", "EDUCATION", "GROCERIES");
    private static final Set<String> DISCRETIONARY = Set.of("SHOPPING", "CLOTHING", "ENTERTAINMENT");

    private final CurrentUserService currentUserService;
    private final AccountRepository accountRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final BillRepository billRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final AutopayAuthorizationRepository autopayRepository;

    public FinancialSnapshotService(
            CurrentUserService currentUserService,
            AccountRepository accountRepository,
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            BillRepository billRepository,
            SavingsGoalRepository savingsGoalRepository,
            AutopayAuthorizationRepository autopayRepository) {
        this.currentUserService = currentUserService;
        this.accountRepository = accountRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.billRepository = billRepository;
        this.savingsGoalRepository = savingsGoalRepository;
        this.autopayRepository = autopayRepository;
    }

    @Transactional(readOnly = true)
    public FinancialSnapshot current() {
        User user = currentUserService.get();
        BigDecimal accountBalance = zero(accountRepository.sumBalancesByUserId(user.getId()));
        BigDecimal walletBalance = walletRepository.findByUserId(user.getId())
                .map(wallet -> zero(wallet.getBalance()))
                .orElse(BigDecimal.ZERO);

        LocalDateTime since = LocalDateTime.now().minusMonths(3);
        List<Transaction> transactions = transactionRepository.findSuccessfulByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .filter(transaction -> !transaction.getCreatedAt().isBefore(since))
                .toList();

        BigDecimal income = monthlyAverage(sum(transactions, "DEPOSIT", "TRANSFER_IN"));
        BigDecimal spending = monthlyAverage(sum(transactions, "WITHDRAWAL", "TRANSFER_OUT"));

        Map<String, BigDecimal> categoryTotals = transactions.stream()
                .filter(transaction -> "WITHDRAWAL".equals(transaction.getType()) || "TRANSFER_OUT".equals(transaction.getType()))
                .collect(Collectors.groupingBy(
                        transaction -> transaction.getCategory() == null ? "OTHER" : transaction.getCategory(),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)));

        List<SpendingBreakdown> breakdown = categoryTotals.entrySet().stream()
                .map(entry -> new SpendingBreakdown(
                        entry.getKey(),
                        monthlyAverage(entry.getValue()),
                        necessityFor(entry.getKey()),
                        DISCRETIONARY.contains(entry.getKey()) || "FOOD".equals(entry.getKey()) || "OTHER".equals(entry.getKey())))
                .sorted(Comparator.comparing(SpendingBreakdown::monthlyAmount).reversed())
                .toList();

        BigDecimal essential = breakdown.stream()
                .filter(item -> !item.reducible())
                .map(SpendingBreakdown::monthlyAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discretionary = breakdown.stream()
                .filter(SpendingBreakdown::reducible)
                .map(SpendingBreakdown::monthlyAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal monthlyBills = billRepository.findByUserIdOrderByNextDueDateAsc(user.getId()).stream()
                .filter(Bill::isActive)
                .map(bill -> monthlyAmount(bill.getAmount(), bill.getFrequency()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal monthlyAutopay = autopayRepository.findByUserIdOrderByNextPaymentDateAsc(user.getId()).stream()
                .filter(this::isActiveObligation)
                .map(authorization -> monthlyAmount(authorization.getAmount(), authorization.getFrequency()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal goalRemaining = savingsGoalRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .filter(goal -> !"COMPLETED".equalsIgnoreCase(goal.getStatus()))
                .map(this::remaining)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new FinancialSnapshot(
                accountBalance,
                walletBalance,
                accountBalance.add(walletBalance),
                income,
                spending,
                essential.max(monthlyBills),
                discretionary,
                monthlyBills,
                monthlyAutopay,
                goalRemaining,
                breakdown);
    }

    private BigDecimal sum(List<Transaction> transactions, String... types) {
        Set<String> allowed = Set.of(types);
        return transactions.stream()
                .filter(transaction -> allowed.contains(transaction.getType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal monthlyAverage(BigDecimal threeMonthAmount) {
        return zero(threeMonthAmount).divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal monthlyAmount(BigDecimal amount, String frequency) {
        if (amount == null) return BigDecimal.ZERO;
        if ("WEEKLY".equalsIgnoreCase(frequency)) return amount.multiply(BigDecimal.valueOf(4));
        if ("QUARTERLY".equalsIgnoreCase(frequency)) return amount.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
        if ("YEARLY".equalsIgnoreCase(frequency) || "ANNUAL".equalsIgnoreCase(frequency)) {
            return amount.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        }
        return amount;
    }

    private boolean isActiveObligation(AutopayAuthorization authorization) {
        return "ACTIVE".equalsIgnoreCase(authorization.getStatus())
                || "PENDING_APPROVAL".equalsIgnoreCase(authorization.getStatus());
    }

    private BigDecimal remaining(SavingsGoal goal) {
        return goal.getTargetAmount().subtract(goal.getCurrentAmount()).max(BigDecimal.ZERO);
    }

    private String necessityFor(String category) {
        if (ESSENTIAL.contains(category)) return "NECESSARY";
        if ("FOOD".equals(category) || "TRANSPORT".equals(category)) return "IMPORTANT";
        if (DISCRETIONARY.contains(category)) return "DISCRETIONARY";
        return "OPTIONAL";
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
