package com.finflow.service;

import com.finflow.dto.FinancialHealthResponse;
import com.finflow.dto.HealthFactorResponse;
import com.finflow.dto.SavingsGoalResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class FinancialHealthService {
    private final AnalyticsService analyticsService;
    private final BudgetService budgetService;
    private final BillService billService;
    private final SavingsGoalService goalService;

    public FinancialHealthService(AnalyticsService analyticsService,
                                  BudgetService budgetService,
                                  BillService billService,
                                  SavingsGoalService goalService) {
        this.analyticsService = analyticsService;
        this.budgetService = budgetService;
        this.billService = billService;
        this.goalService = goalService;
    }

    public FinancialHealthResponse getScore() {
        var summary = analyticsService.getSummary();
        List<HealthFactorResponse> factors = new ArrayList<>();

        BigDecimal income = summary.getTotalIncome();
        BigDecimal expense = summary.getTotalExpense();
        int savingsScore;
        String savingsExplanation;
        if (income.signum() == 0) {
            savingsScore = 12;
            savingsExplanation = "No income is recorded yet; this factor starts neutral.";
        } else {
            BigDecimal rate = income.subtract(expense).multiply(BigDecimal.valueOf(100))
                    .divide(income, 2, RoundingMode.HALF_UP);
            savingsScore = rate.signum() < 0 ? 0
                    : rate.compareTo(BigDecimal.valueOf(20)) >= 0 ? 25
                    : rate.compareTo(BigDecimal.TEN) >= 0 ? 18
                    : rate.signum() > 0 ? 10 : 5;
            savingsExplanation = "Recorded net income is " + rate.stripTrailingZeros().toPlainString()
                    + "% of recorded income.";
        }
        factors.add(new HealthFactorResponse("Savings rate", savingsScore, 25, savingsExplanation));

        var budgets = budgetService.list().stream()
                .filter(budget -> "ACTIVE".equals(budget.periodStatus())).toList();
        int budgetScore;
        String budgetExplanation;
        if (budgets.isEmpty()) {
            budgetScore = 12;
            budgetExplanation = "No active budgets yet; this factor starts neutral.";
        } else {
            BigDecimal averageUsed = budgets.stream()
                    .map(budget -> budget.percentageUsed().min(BigDecimal.valueOf(100)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(budgets.size()), 2, RoundingMode.HALF_UP);
            budgetScore = averageUsed.compareTo(BigDecimal.valueOf(75)) <= 0 ? 25
                    : averageUsed.compareTo(BigDecimal.valueOf(90)) <= 0 ? 18
                    : averageUsed.compareTo(BigDecimal.valueOf(100)) <= 0 ? 10 : 0;
            budgetExplanation = "Active budgets are " + averageUsed.stripTrailingZeros().toPlainString()
                    + "% used on average.";
        }
        factors.add(new HealthFactorResponse("Budget adherence", budgetScore, 25, budgetExplanation));

        var bills = billService.dueWithinDays(30);
        BigDecimal obligations = bills.stream().map(bill -> bill.amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int billScore;
        String billExplanation;
        if (bills.isEmpty()) {
            billScore = 25;
            billExplanation = "No active bills are due within the next 30 days.";
        } else {
            BigDecimal available = summary.getCurrentBalance();
            billScore = available.compareTo(obligations) >= 0 ? 25
                    : available.compareTo(obligations.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)) >= 0 ? 15 : 5;
            billExplanation = "Available balance covers "
                    + available.multiply(BigDecimal.valueOf(100))
                    .divide(obligations, 2, RoundingMode.HALF_UP)
                    .min(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString()
                    + "% of bills due within 30 days.";
        }
        factors.add(new HealthFactorResponse("Upcoming bill coverage", billScore, 25, billExplanation));

        List<SavingsGoalResponse> goals = goalService.list();
        int goalScore;
        String goalExplanation;
        if (goals.isEmpty()) {
            goalScore = 12;
            goalExplanation = "No savings goals yet; this factor starts neutral.";
        } else {
            BigDecimal averageProgress = goals.stream()
                    .map(SavingsGoalResponse::percentageComplete)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(goals.size()), 2, RoundingMode.HALF_UP);
            goalScore = averageProgress.compareTo(BigDecimal.valueOf(75)) >= 0 ? 25
                    : averageProgress.compareTo(BigDecimal.valueOf(50)) >= 0 ? 19
                    : averageProgress.compareTo(BigDecimal.valueOf(25)) >= 0 ? 13 : 7;
            goalExplanation = "Savings goals are " + averageProgress.stripTrailingZeros().toPlainString()
                    + "% complete on average.";
        }
        factors.add(new HealthFactorResponse("Savings goal progress", goalScore, 25, goalExplanation));

        int score = factors.stream().mapToInt(HealthFactorResponse::score).sum();
        String rating = score >= 80 ? "STRONG" : score >= 60 ? "GOOD"
                : score >= 40 ? "FAIR" : "NEEDS ATTENTION";
        return new FinancialHealthResponse(score, rating, List.copyOf(factors));
    }
}
