package com.finflow.service;

import com.finflow.dto.*;
import com.finflow.entity.AutopayAuthorization;
import com.finflow.entity.Bill;
import com.finflow.entity.SavingsGoal;
import com.finflow.entity.Transaction;
import com.finflow.entity.User;
import com.finflow.insight.InsightNarrationProvider;
import com.finflow.insight.NarrationUnavailableException;
import com.finflow.repository.AutopayAuthorizationRepository;
import com.finflow.repository.BillRepository;
import com.finflow.repository.SavingsGoalRepository;
import com.finflow.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class MonthlyReportService {

    private static final Set<String> INCOME = Set.of("DEPOSIT", "TRANSFER_IN");
    private static final Set<String> EXPENSE = Set.of("WITHDRAWAL", "TRANSFER_OUT");

    private final TransactionRepository transactionRepository;
    private final BillRepository billRepository;
    private final AutopayAuthorizationRepository autopayRepository;
    private final SavingsGoalRepository goalRepository;
    private final InsightNarrationProvider narrationProvider;

    public MonthlyReportService(TransactionRepository transactionRepository, BillRepository billRepository,
                                AutopayAuthorizationRepository autopayRepository, SavingsGoalRepository goalRepository,
                                InsightNarrationProvider narrationProvider) {
        this.transactionRepository = transactionRepository;
        this.billRepository = billRepository;
        this.autopayRepository = autopayRepository;
        this.goalRepository = goalRepository;
        this.narrationProvider = narrationProvider;
    }

    @Transactional(readOnly = true)
    public MonthlyReportResponse build(User user, YearMonth month) {
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();
        String label = month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + month.getYear();

        List<Transaction> monthly = transactionRepository.findSuccessfulByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .filter(transaction -> transaction.getCreatedAt() != null
                        && YearMonth.from(transaction.getCreatedAt()).equals(month))
                .toList();

        BigDecimal totalIn = sum(monthly, INCOME);
        BigDecimal totalOut = sum(monthly, EXPENSE);
        BigDecimal net = totalIn.subtract(totalOut);
        long count = monthly.size();
        boolean hasActivity = count > 0;

        List<MonthlyCategoryLine> topCategories = topCategories(monthly);

        List<Bill> billsDue = billRepository.findByUserIdAndActiveTrueAndNextDueDateBetweenOrderByNextDueDateAsc(user.getId(), monthStart, monthEnd);
        List<MonthlyBillLine> bills = billsDue.stream()
                .map(bill -> new MonthlyBillLine(bill.getName(), bill.getCategory(), bill.getAmount(), bill.getNextDueDate()))
                .toList();
        BigDecimal billsTotal = billsDue.stream().map(Bill::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<AutopayAuthorization> autopays = autopayRepository.findByUserIdOrderByNextPaymentDateAsc(user.getId()).stream()
                .filter(autopay -> "ACTIVE".equalsIgnoreCase(autopay.getStatus()))
                .toList();
        BigDecimal autopayAmount = autopays.stream().map(AutopayAuthorization::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<MonthlyGoalLine> goals = goalRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::goalLine)
                .toList();

        String deterministic = deterministicSummary(label, hasActivity, totalIn, totalOut, net, count, topCategories);
        String narration = narrate(deterministic);
        boolean aiGenerated = narration != null && !narration.equals(deterministic) && narrationProvider.available();
        String source = !narrationProvider.available() ? "NONE" : (aiGenerated ? "AI" : "FALLBACK");
        String message = hasActivity ? "Your FinFlow summary for " + label + "." : "No transactions were recorded in " + label + ".";

        return new MonthlyReportResponse(month.toString(), label, hasActivity, totalIn, totalOut, net, count,
                topCategories, bills, billsTotal, autopays.size(), autopayAmount, goals,
                narration, aiGenerated, source, message);
    }

    private BigDecimal sum(List<Transaction> transactions, Set<String> types) {
        return transactions.stream()
                .filter(transaction -> types.contains(transaction.getType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<MonthlyCategoryLine> topCategories(List<Transaction> transactions) {
        Map<String, BigDecimal> amounts = new LinkedHashMap<>();
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Transaction transaction : transactions) {
            if (!EXPENSE.contains(transaction.getType())) continue;
            String category = transaction.getCategory() == null || transaction.getCategory().isBlank() ? "OTHER" : transaction.getCategory();
            amounts.merge(category, transaction.getAmount(), BigDecimal::add);
            counts.merge(category, 1L, Long::sum);
        }
        List<MonthlyCategoryLine> lines = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : amounts.entrySet()) {
            lines.add(new MonthlyCategoryLine(entry.getKey(), entry.getValue(), counts.get(entry.getKey())));
        }
        lines.sort(Comparator.comparing(MonthlyCategoryLine::amount).reversed());
        return lines.size() > 5 ? lines.subList(0, 5) : lines;
    }

    private MonthlyGoalLine goalLine(SavingsGoal goal) {
        BigDecimal target = goal.getTargetAmount();
        BigDecimal current = goal.getCurrentAmount() == null ? BigDecimal.ZERO : goal.getCurrentAmount();
        BigDecimal percent = target == null || target.signum() == 0
                ? BigDecimal.ZERO
                : current.multiply(BigDecimal.valueOf(100)).divide(target, 2, RoundingMode.HALF_UP);
        return new MonthlyGoalLine(goal.getName(), current, target, percent);
    }

    private String narrate(String deterministic) {
        if (!narrationProvider.available()) return deterministic;
        try {
            String prompt = "Rewrite this monthly finance summary as 2 short, friendly sentences for a personal finance app. "
                    + "Do not change or invent any numbers; use only what is given. Summary: " + deterministic;
            String narrated = narrationProvider.narrate(prompt);
            return narrated == null || narrated.isBlank() ? deterministic : narrated.trim();
        } catch (NarrationUnavailableException exception) {
            return deterministic;
        }
    }

    private String deterministicSummary(String label, boolean hasActivity, BigDecimal totalIn, BigDecimal totalOut,
                                        BigDecimal net, long count, List<MonthlyCategoryLine> topCategories) {
        if (!hasActivity) {
            return "No transactions were recorded in " + label + ".";
        }
        StringBuilder text = new StringBuilder();
        text.append("In ").append(label).append(" you received ").append(money(totalIn))
                .append(" and spent ").append(money(totalOut))
                .append(", a net of ").append(money(net))
                .append(" across ").append(count).append(count == 1 ? " transaction." : " transactions.");
        if (!topCategories.isEmpty()) {
            MonthlyCategoryLine top = topCategories.get(0);
            text.append(" Your largest category was ").append(label(top.category())).append(" at ").append(money(top.amount())).append(".");
        }
        return text.toString();
    }

    private String money(BigDecimal value) {
        if (value == null) return "₹0.00";
        return "₹" + value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String label(String category) {
        if (category == null || category.isBlank()) return "Other";
        String lower = category.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
