package com.finflow.service;

import com.finflow.dto.BillResponse;
import com.finflow.dto.BudgetResponse;
import com.finflow.dto.FinancialHealthResponse;
import com.finflow.dto.NotificationResponse;
import com.finflow.dto.SavingsGoalResponse;
import com.finflow.entity.FinanceNotification;
import com.finflow.entity.Transaction;
import com.finflow.entity.User;
import com.finflow.exception.ResourceNotFoundException;
import com.finflow.repository.AccountRepository;
import com.finflow.repository.AutopayAuthorizationRepository;
import com.finflow.repository.BillRepository;
import com.finflow.repository.BudgetRepository;
import com.finflow.repository.FinanceNotificationRepository;
import com.finflow.repository.SavingsGoalRepository;
import com.finflow.repository.TransactionRepository;
import com.finflow.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import com.finflow.dto.PageResponse;

@Service
public class FinanceNotificationService {
    public static String scheduledEventKey(String type, Long id, String marker) {
        return type + ":" + id + ":" + marker;
    }
    private final FinanceNotificationRepository notificationRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CurrentUserService currentUserService;
    private final BudgetService budgetService;
    private final BillService billService;
    private final SavingsGoalService goalService;
    private final AnalyticsService analyticsService;
    private final AutopayAuthorizationRepository autopayRepository;
    private final BillRepository billRepository;
    private final BudgetRepository budgetRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final UserRepository userRepository;

    public FinanceNotificationService(FinanceNotificationRepository notificationRepository,
                                      TransactionRepository transactionRepository,
                                      AccountRepository accountRepository,
                                      CurrentUserService currentUserService,
                                      BudgetService budgetService,
                                      BillService billService,
                                      SavingsGoalService goalService,
                                      AnalyticsService analyticsService,
                                      AutopayAuthorizationRepository autopayRepository,
                                      BillRepository billRepository,
                                      BudgetRepository budgetRepository,
                                      SavingsGoalRepository savingsGoalRepository,
                                      UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.currentUserService = currentUserService;
        this.budgetService = budgetService;
        this.billService = billService;
        this.goalService = goalService;
        this.analyticsService = analyticsService;
        this.autopayRepository = autopayRepository;
        this.billRepository = billRepository;
        this.budgetRepository = budgetRepository;
        this.savingsGoalRepository = savingsGoalRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public List<NotificationResponse> list(boolean unreadOnly) {
        User user = currentUserService.get();
        generate(user);
        List<FinanceNotification> notifications = unreadOnly
                ? notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(user.getId())
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return notifications.stream().map(this::toResponse).toList();
    }

    @Transactional
    public PageResponse<NotificationResponse> list(boolean unreadOnly, int page, int size) {
        User user = currentUserService.get();
        generate(user);
        var result = unreadOnly
                ? notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(user.getId(), PageRequest.of(page, size))
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(page, size));
        return new PageResponse<>(result.getContent().stream().map(this::toResponse).toList(), page, size,
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public NotificationResponse markRead(Long id) {
        return setRead(id, true);
    }

    @Transactional
    public NotificationResponse setRead(Long id, boolean read) {
        User user = currentUserService.get();
        FinanceNotification notification = notificationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.setRead(read);
        return toResponse(notificationRepository.save(notification));
    }

    @Transactional
    public long markAllRead() {
        User user = currentUserService.get();
        List<FinanceNotification> unread =
                notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(user.getId());
        unread.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(unread);
        return unread.size();
    }

    @Transactional
    public long unreadCount() {
        User user = currentUserService.get();
        generate(user);
        return notificationRepository.countByUserIdAndReadFalse(user.getId());
    }

    @Transactional
    public void generateForCurrentUser() {
        generate(currentUserService.get());
    }

    public static final int USER_BATCH_SIZE = 200;

    @Transactional
    public Long generateForUserBatch(Long afterUserId) {
        List<User> users = userRepository.findByIdGreaterThanOrderByIdAsc(
                afterUserId == null ? 0L : afterUserId, PageRequest.of(0, USER_BATCH_SIZE));
        users.forEach(this::generateScheduledNotifications);
        return users.size() < USER_BATCH_SIZE ? null : users.get(users.size() - 1).getId();
    }

    private void generate(User user) {
        LocalDate today = LocalDate.now();
        BigDecimal balance = accountRepository.sumBalancesByUserId(user.getId());
        if (balance != null && balance.compareTo(BigDecimal.valueOf(1000)) < 0) {
            create(user, "LOW_BALANCE:" + today, "LOW_BALANCE", "Low balance",
                    "Your combined account balance is " + money(balance) + ".",
                    "WARNING");
        }

        for (BudgetResponse budget : budgetService.list()) {
            if (!"ACTIVE".equals(budget.periodStatus())) continue;
            BigDecimal used = budget.percentageUsed();
            for (int threshold : List.of(75, 90, 100)) {
                if (used.compareTo(BigDecimal.valueOf(threshold)) < 0) continue;
                boolean exceeded = threshold == 100;
                create(user, "BUDGET:" + budget.id() + ":" + threshold,
                        exceeded ? "BUDGET_EXCEEDED" : "BUDGET_WARNING",
                        exceeded ? "Budget limit reached" : "Budget getting close",
                        exceeded
                                ? "Your " + budget.category() + " budget has reached or exceeded its limit."
                                : "You have used at least " + threshold + "% of your "
                                  + budget.category() + " budget.",
                        exceeded || threshold >= 90 ? "WARNING" : "INFO");
            }
        }

        for (BillResponse bill : billService.dueWithinDays(7)) {
            if (!bill.reminderEnabled()) continue;
            String dueText = bill.daysUntilDue() < 0
                    ? "is overdue by " + Math.abs(bill.daysUntilDue()) + " days"
                    : bill.daysUntilDue() == 0 ? "is due today"
                    : "is due in " + bill.daysUntilDue() + " days";
            create(user, "BILL:" + bill.id() + ":" + bill.nextDueDate(),
                    "UPCOMING_BILL", "Bill due soon",
                    bill.name() + " of " + money(bill.amount()) + " " + dueText + ".",
                    bill.daysUntilDue() <= 1 ? "WARNING" : "INFO");
        }

        for (SavingsGoalResponse goal : goalService.list()) {
            if (goal.daysRemaining() != null && goal.daysRemaining() < 0
                    && goal.remainingAmount().signum() > 0) {
                create(user, "GOAL_DELAY:" + goal.id() + ":" + goal.targetDate(),
                        "GOAL_DELAY", "Savings goal needs attention",
                        "Your " + goal.name() + " goal is past its target date and remains "
                                + money(goal.remainingAmount()) + ".",
                        "INFO");
            }
        }

        generateSpendingIncrease(user);
        generateUnusualTransactionAlerts(user);
        generateScheduledNotifications(user);
    }

    private void generateScheduledNotifications(User user) {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        autopayRepository.findByUserIdOrderByNextPaymentDateAsc(user.getId()).forEach(autopay -> {
            if ("ACTIVE".equalsIgnoreCase(autopay.getStatus())
                    && autopay.getNextPaymentDate() != null
                    && !autopay.getNextPaymentDate().isBefore(today)
                    && !autopay.getNextPaymentDate().isAfter(tomorrow)) {
                create(user, "AUTOPAY_DUE_24H:" + autopay.getId() + ":" + autopay.getNextPaymentDate(),
                        "AUTOPAY_DUE_24H", "Autopay due soon",
                        autopay.getMerchantName() + " autopay of " + money(autopay.getAmount()) + " is due within 24 hours.", "WARNING");
            }
            if ("FAILED".equalsIgnoreCase(autopay.getLastPaymentStatus())) {
                create(user, "AUTOPAY_FAILED:" + autopay.getId() + ":" + autopay.getUpdatedAt(),
                        "AUTOPAY_FAILED", "Autopay payment failed",
                        "The latest payment for " + autopay.getMerchantName() + " could not be completed.", "WARNING");
            }
        });

        budgetRepository.findByUserIdOrderByStartDateDesc(user.getId()).forEach(budget -> {
            if (today.isBefore(budget.getStartDate()) || today.isAfter(budget.getEndDate())) return;
            BigDecimal spent = transactionRepository.sumExpensesForBudget(user.getId(), budget.getCategory(),
                    budget.getStartDate().atStartOfDay(), budget.getEndDate().plusDays(1).atStartOfDay().minusNanos(1));
            BudgetProjectionCalculator.Result projection = BudgetProjectionCalculator.calculate(
                    budget.getBudgetAmount(), spent, budget.getStartDate(), budget.getEndDate(), today);
            if (!"OK".equals(projection.status())) {
                create(user, "BUDGET_PROJECTION:" + budget.getId() + ":" + projection.status(),
                        "BUDGET_WARNING", "Budget needs attention",
                        budget.getCategory() + " is at " + projection.percentUsed().stripTrailingZeros().toPlainString()
                                + "% used; projection status is " + projection.status() + ".", "WARNING");
            }
        });

        savingsGoalRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).forEach(goal -> {
            BigDecimal percentage = goal.getCurrentAmount().multiply(BigDecimal.valueOf(100))
                    .divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP);
            for (int milestone : List.of(50, 75, 100)) {
                if (percentage.compareTo(BigDecimal.valueOf(milestone)) >= 0) {
                    create(user, scheduledEventKey("GOAL_MILESTONE", goal.getId(), String.valueOf(milestone)),
                            "GOAL_MILESTONE", "Savings goal milestone",
                            goal.getName() + " reached " + milestone + "% of its target.", "INFO");
                }
            }
        });
    }

    private void generateSpendingIncrease(User user) {
        var months = analyticsService.getMonthlyAnalytics();
        if (months.size() < 2) return;
        var current = months.get(months.size() - 1);
        var previous = months.get(months.size() - 2);
        BigDecimal previousExpense = previous.getExpense();
        BigDecimal currentExpense = current.getExpense();
        if (previousExpense.signum() <= 0) return;
        BigDecimal increase = currentExpense.subtract(previousExpense)
                .multiply(BigDecimal.valueOf(100))
                .divide(previousExpense, 2, RoundingMode.HALF_UP);
        if (increase.compareTo(BigDecimal.valueOf(30)) >= 0) {
            create(user, "SPENDING_INCREASE:" + current.getMonth(),
                    "SPENDING_INCREASE", "Spending increased",
                    "Your spending increased by " + increase.stripTrailingZeros().toPlainString()
                            + "% compared with " + previous.getMonth() + ".",
                    "INFO");
        }
    }

    private void generateUnusualTransactionAlerts(User user) {
        List<Transaction> all = transactionRepository
                .findSuccessfulByUserIdOrderByCreatedAtDesc(user.getId());
        List<Transaction> recent = all.stream().limit(20).toList();
        for (Transaction candidate : recent) {
            if (!"WITHDRAWAL".equals(candidate.getType())
                    && !"TRANSFER_OUT".equals(candidate.getType())) continue;
            if (candidate.getCategory() == null) continue;
            List<BigDecimal> previousAmounts = new ArrayList<>();
            for (Transaction prior : all) {
                if (!prior.getCreatedAt().isBefore(candidate.getCreatedAt())) continue;
                if (!candidate.getCategory().equals(prior.getCategory())) continue;
                if ("WITHDRAWAL".equals(prior.getType()) || "TRANSFER_OUT".equals(prior.getType())) {
                    previousAmounts.add(prior.getAmount());
                }
            }
            if (previousAmounts.size() < 3) continue;
            BigDecimal total = previousAmounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal average = total.divide(BigDecimal.valueOf(previousAmounts.size()), 2, RoundingMode.HALF_UP);
            if (candidate.getAmount().compareTo(BigDecimal.valueOf(1000)) >= 0
                    && candidate.getAmount().compareTo(average.multiply(BigDecimal.valueOf(3))) > 0) {
                create(user, "UNUSUAL_TRANSACTION:" + candidate.getId(),
                        "UNUSUAL_TRANSACTION", "Unusual transaction detected",
                        "An unusual " + candidate.getCategory() + " transaction of "
                                + money(candidate.getAmount()) + " was detected compared with your past "
                                + candidate.getCategory() + " activity.",
                        "WARNING");
            }
        }
    }

    private void create(User user, String key, String type, String title,
                        String message, String severity) {
        notificationRepository.createIfAbsent(
                user.getId(), key, type, title, message, severity);
    }

    private NotificationResponse toResponse(FinanceNotification notification) {
        return new NotificationResponse(notification.getId(), notification.getType(),
                notification.getTitle(), notification.getMessage(),
                notification.getSeverity(), notification.isRead(),
                notification.getCreatedAt());
    }

    private String money(BigDecimal value) {
        return "₹" + value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
