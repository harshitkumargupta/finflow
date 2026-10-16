package com.finflow.service;

import com.finflow.dto.AnalyticsSummaryResponse;
import com.finflow.dto.CategoryAnalyticsResponse;
import com.finflow.dto.MonthlyAnalyticsResponse;
import com.finflow.entity.Account;
import com.finflow.entity.Transaction;
import com.finflow.entity.User;
import com.finflow.exception.ResourceNotFoundException;
import com.finflow.repository.AccountRepository;
import com.finflow.repository.TransactionRepository;
import com.finflow.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class AnalyticsService {

    private static final String SUCCESS = "SUCCESS";
    private static final String DEPOSIT = "DEPOSIT";
    private static final String WITHDRAWAL = "WITHDRAWAL";
    private static final String TRANSFER_IN = "TRANSFER_IN";
    private static final String TRANSFER_OUT = "TRANSFER_OUT";

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public AnalyticsService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse getSummary() {
        User authenticatedUser = getAuthenticatedUser();
        BigDecimal currentBalance =
                accountRepository.sumBalancesByUserId(authenticatedUser.getId());
        Object[] totals = getSuccessfulTotals(authenticatedUser.getId());

        BigDecimal totalDeposits = asBigDecimal(totals[0]);
        BigDecimal totalWithdrawals = asBigDecimal(totals[1]);
        BigDecimal totalTransfersIn = asBigDecimal(totals[2]);
        BigDecimal totalTransfersOut = asBigDecimal(totals[3]);

        return new AnalyticsSummaryResponse(
                currentBalance == null ? BigDecimal.ZERO : currentBalance,
                totalDeposits.add(totalTransfersIn),
                totalWithdrawals.add(totalTransfersOut),
                totalDeposits,
                totalWithdrawals,
                totalTransfersIn,
                totalTransfersOut,
                ((Number) totals[4]).longValue()
        );
    }

    @Transactional(readOnly = true)
    public List<CategoryAnalyticsResponse> getCategoryAnalytics() {
        User authenticatedUser = getAuthenticatedUser();
        Object[] totals = getSuccessfulTotals(authenticatedUser.getId());
        BigDecimal totalExpense =
                asBigDecimal(totals[1]).add(asBigDecimal(totals[3]));

        List<Object[]> categoryTotals =
                transactionRepository.getSuccessfulExpenseTotalsByCategory(
                        authenticatedUser.getId()
                );
        List<CategoryAnalyticsResponse> response = new ArrayList<>();

        for (Object[] row : categoryTotals) {
            BigDecimal amount = asBigDecimal(row[1]);
            BigDecimal percentage = totalExpense.signum() == 0
                    ? BigDecimal.ZERO
                    : amount.multiply(BigDecimal.valueOf(100))
                            .divide(totalExpense, 2, RoundingMode.HALF_UP);
            response.add(new CategoryAnalyticsResponse(
                    (String) row[0],
                    amount,
                    ((Number) row[2]).longValue(),
                    percentage
            ));
        }

        response.sort(
                Comparator.comparing(CategoryAnalyticsResponse::getTotalAmount)
                        .reversed()
                        .thenComparing(CategoryAnalyticsResponse::getCategory)
        );
        return response;
    }

    @Transactional(readOnly = true)
    public List<MonthlyAnalyticsResponse> getMonthlyAnalytics() {
        User authenticatedUser = getAuthenticatedUser();
        List<Object[]> monthlyTotals =
                transactionRepository.getSuccessfulMonthlyTotalsByUserId(
                        authenticatedUser.getId()
                );
        List<MonthlyAnalyticsResponse> response = new ArrayList<>();

        for (Object[] row : monthlyTotals) {
            BigDecimal deposits = asBigDecimal(row[1]);
            BigDecimal withdrawals = asBigDecimal(row[2]);
            BigDecimal transfersIn = asBigDecimal(row[3]);
            BigDecimal transfersOut = asBigDecimal(row[4]);
            BigDecimal income = deposits.add(transfersIn);
            BigDecimal expense = withdrawals.add(transfersOut);

            response.add(new MonthlyAnalyticsResponse(
                    (String) row[0],
                    income,
                    expense,
                    income.subtract(expense),
                    deposits,
                    withdrawals,
                    transfersIn,
                    transfersOut,
                    ((Number) row[5]).longValue()
            ));
        }
        return response;
    }

    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse getSummary(Long accountId) {
        Account account = getOwnedAccount(accountId);
        List<Transaction> transactions =
                transactionRepository.findByAccountIdAndStatus(accountId, SUCCESS);

        BigDecimal totalDeposits = BigDecimal.ZERO;
        BigDecimal totalWithdrawals = BigDecimal.ZERO;
        BigDecimal totalTransfersIn = BigDecimal.ZERO;
        BigDecimal totalTransfersOut = BigDecimal.ZERO;

        for (Transaction transaction : transactions) {
            BigDecimal amount = transaction.getAmount();
            switch (transaction.getType()) {
                case DEPOSIT -> totalDeposits = totalDeposits.add(amount);
                case WITHDRAWAL -> totalWithdrawals = totalWithdrawals.add(amount);
                case TRANSFER_IN -> totalTransfersIn = totalTransfersIn.add(amount);
                case TRANSFER_OUT -> totalTransfersOut = totalTransfersOut.add(amount);
                default -> {
                }
            }
        }

        BigDecimal totalIncome = totalDeposits.add(totalTransfersIn);
        BigDecimal totalExpense = totalWithdrawals.add(totalTransfersOut);

        return new AnalyticsSummaryResponse(
                account.getBalance(),
                totalIncome,
                totalExpense,
                totalDeposits,
                totalWithdrawals,
                totalTransfersIn,
                totalTransfersOut,
                transactions.size()
        );
    }

    @Transactional(readOnly = true)
    public List<CategoryAnalyticsResponse> getCategories(Long accountId) {
        getOwnedAccount(accountId);
        List<Transaction> transactions =
                transactionRepository.findByAccountIdAndStatus(accountId, SUCCESS);
        Map<String, CategoryTotals> totalsByCategory = new HashMap<>();
        BigDecimal totalExpense = BigDecimal.ZERO;

        for (Transaction transaction : transactions) {
            if (!WITHDRAWAL.equals(transaction.getType())
                    && !TRANSFER_OUT.equals(transaction.getType())) {
                continue;
            }
            totalExpense = totalExpense.add(transaction.getAmount());

            String category = transaction.getCategory();
            if (category == null || category.isBlank()) {
                continue;
            }

            CategoryTotals totals = totalsByCategory.computeIfAbsent(
                    category,
                    ignored -> new CategoryTotals()
            );
            totals.totalAmount = totals.totalAmount.add(transaction.getAmount());
            totals.transactionCount++;
        }

        List<CategoryAnalyticsResponse> response = new ArrayList<>();
        for (Map.Entry<String, CategoryTotals> entry : totalsByCategory.entrySet()) {
            CategoryTotals totals = entry.getValue();
            BigDecimal percentage = totalExpense.signum() == 0
                    ? BigDecimal.ZERO
                    : totals.totalAmount.multiply(BigDecimal.valueOf(100))
                            .divide(totalExpense, 2, RoundingMode.HALF_UP);
            response.add(new CategoryAnalyticsResponse(
                    entry.getKey(),
                    totals.totalAmount,
                    totals.transactionCount,
                    percentage
            ));
        }

        response.sort(
                Comparator.comparing(CategoryAnalyticsResponse::getTotalAmount)
                        .reversed()
                        .thenComparing(CategoryAnalyticsResponse::getCategory)
        );
        return response;
    }

    @Transactional(readOnly = true)
    public List<MonthlyAnalyticsResponse> getMonthly(Long accountId) {
        getOwnedAccount(accountId);
        List<Transaction> transactions =
                transactionRepository.findByAccountIdAndStatus(accountId, SUCCESS);
        Map<YearMonth, MonthlyTotals> totalsByMonth =
                new TreeMap<>(Comparator.reverseOrder());

        for (Transaction transaction : transactions) {
            YearMonth month = YearMonth.from(transaction.getCreatedAt());
            MonthlyTotals totals = totalsByMonth.computeIfAbsent(
                    month,
                    ignored -> new MonthlyTotals()
            );

            switch (transaction.getType()) {
                case DEPOSIT -> {
                    totals.deposits = totals.deposits.add(transaction.getAmount());
                    totals.income = totals.income.add(transaction.getAmount());
                }
                case TRANSFER_IN -> {
                    totals.transfersIn = totals.transfersIn.add(transaction.getAmount());
                        totals.income = totals.income.add(transaction.getAmount());
                }
                case WITHDRAWAL -> {
                    totals.withdrawals = totals.withdrawals.add(transaction.getAmount());
                    totals.expense = totals.expense.add(transaction.getAmount());
                }
                case TRANSFER_OUT -> {
                    totals.transfersOut = totals.transfersOut.add(transaction.getAmount());
                        totals.expense = totals.expense.add(transaction.getAmount());
                }
                default -> {
                }
            }
            totals.transactionCount++;
        }

        List<MonthlyAnalyticsResponse> response = new ArrayList<>();
        for (Map.Entry<YearMonth, MonthlyTotals> entry : totalsByMonth.entrySet()) {
            MonthlyTotals totals = entry.getValue();
            response.add(new MonthlyAnalyticsResponse(
                    entry.getKey().toString(),
                    totals.income,
                    totals.expense,
                    totals.income.subtract(totals.expense),
                    totals.deposits,
                    totals.withdrawals,
                    totals.transfersIn,
                    totals.transfersOut,
                    totals.transactionCount
            ));
        }
        return response;
    }

    private Account getOwnedAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Account not found with id: " + accountId
                        ));

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        User authenticatedUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new AccessDeniedException("Authenticated user not found"));

        if (!authenticatedUser.getId().equals(account.getUser().getId())) {
            throw new AccessDeniedException(
                    "You are not authorized to access this account"
            );
        }
        return account;
    }

    private User getAuthenticatedUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication is required");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new AccessDeniedException("Authenticated user not found"));
    }

    private Object[] getSuccessfulTotals(Long userId) {
        List<Object[]> rows = transactionRepository.getSuccessfulTotalsByUserId(userId);
        return rows.isEmpty() ? new Object[]{BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, 0L} : rows.get(0);
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        return new BigDecimal(value.toString());
    }

    private static class CategoryTotals {
        private BigDecimal totalAmount = BigDecimal.ZERO;
        private long transactionCount;
    }

    private static class MonthlyTotals {
        private BigDecimal income = BigDecimal.ZERO;
        private BigDecimal expense = BigDecimal.ZERO;
        private BigDecimal deposits = BigDecimal.ZERO;
        private BigDecimal withdrawals = BigDecimal.ZERO;
        private BigDecimal transfersIn = BigDecimal.ZERO;
        private BigDecimal transfersOut = BigDecimal.ZERO;
        private long transactionCount;
    }
}
