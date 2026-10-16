package com.finflow.service;

import com.finflow.dto.FinancialSnapshot;
import com.finflow.dto.GoalPlanningRequest;
import com.finflow.dto.GoalRecommendation;
import com.finflow.dto.SpendingBreakdown;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SandboxFinancialAssistantProvider implements FinancialAssistantProvider {
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(?:₹|rs\\.?|inr)?\\s*([0-9][0-9,]*(?:\\.\\d{1,2})?)\\s*(lakh|lac|k)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern MONTH_PATTERN = Pattern.compile("(\\d+)\\s*months?", Pattern.CASE_INSENSITIVE);

    @Override
    public String providerName() {
        return "SANDBOX_RULES";
    }

    @Override
    public GoalRecommendation recommend(GoalPlanningRequest request, FinancialSnapshot snapshot) {
        String prompt = request.prompt() == null ? "" : request.prompt().trim();
        BigDecimal target = request.targetAmount() != null ? request.targetAmount() : parseAmount(prompt);
        if (target == null || target.signum() <= 0) {
            target = BigDecimal.valueOf(50000);
        }

        String goalName = request.goalName() == null || request.goalName().isBlank()
                ? inferGoalName(prompt)
                : request.goalName().trim();
        Integer months = request.targetMonths() != null && request.targetMonths() > 0
                ? request.targetMonths()
                : parseMonths(prompt);
        LocalDate targetDate = request.targetDate();
        if (targetDate == null && months != null) {
            targetDate = LocalDate.now().plusMonths(months);
        }
        if (months == null && targetDate != null && targetDate.isAfter(LocalDate.now())) {
            months = Math.max(1, (int) ChronoUnit.MONTHS.between(LocalDate.now(), targetDate)
                    + (LocalDate.now().plusMonths(ChronoUnit.MONTHS.between(LocalDate.now(), targetDate)).isBefore(targetDate) ? 1 : 0));
        }
        if (months == null) {
            months = 6;
            targetDate = LocalDate.now().plusMonths(months);
        }

        BigDecimal currentSavings = snapshot.currentAvailableMoney().min(target).max(BigDecimal.ZERO);
        BigDecimal remaining = target.subtract(currentSavings).max(BigDecimal.ZERO);
        BigDecimal requiredMonthly = remaining.divide(BigDecimal.valueOf(Math.max(months, 1)), 2, RoundingMode.CEILING);
        BigDecimal requiredWeekly = requiredMonthly.divide(BigDecimal.valueOf(4), 2, RoundingMode.CEILING);
        BigDecimal surplus = snapshot.averageMonthlyIncome()
                .subtract(snapshot.monthlyEssentialSpending())
                .subtract(snapshot.monthlyAutopayObligations())
                .subtract(snapshot.monthlyBills())
                .subtract(snapshot.existingGoalRemaining().divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP))
                .max(BigDecimal.ZERO);
        boolean achievable = remaining.signum() == 0 || surplus.compareTo(requiredMonthly) >= 0;
        BigDecimal chosenMonthlySaving = request.chosenMonthlySaving() != null
                ? request.chosenMonthlySaving()
                : requiredMonthly.min(surplus.signum() > 0 ? surplus : requiredMonthly);
        LocalDate estimatedCompletion = remaining.signum() == 0
                ? LocalDate.now()
                : LocalDate.now().plusMonths(monthsToComplete(remaining, surplus));
        LocalDate projectedCompletion = remaining.signum() == 0
                ? LocalDate.now()
                : LocalDate.now().plusMonths(monthsToComplete(remaining, chosenMonthlySaving));
        String necessity = classifyNecessity(goalName, prompt);

        List<String> keepProtected = List.of("Rent", "Bills", "Medical", "Education", "Groceries", "Existing mandatory autopay");
        List<String> reduce = snapshot.spendingBreakdown().stream()
                .filter(SpendingBreakdown::reducible)
                .map(SpendingBreakdown::category)
                .distinct()
                .limit(4)
                .toList();
        if (reduce.isEmpty()) {
            reduce = List.of("Shopping", "Entertainment", "Food delivery", "Other discretionary spending");
        }
        List<String> alternatives = new ArrayList<>();
        if (!achievable) {
            alternatives.add("Extend the target date");
            alternatives.add("Reduce the target amount");
            alternatives.add("Reduce discretionary spending where comfortable");
            alternatives.add("Add new income or sandbox deposits before increasing obligations");
        }
        List<String> suggestedActions = new ArrayList<>();
        suggestedActions.add("Save " + money(chosenMonthlySaving) + " per month toward this goal.");
        suggestedActions.add("Keep essential expenses and mandatory autopay obligations protected.");
        if (!reduce.isEmpty()) {
            suggestedActions.add("Review " + String.join(", ", reduce) + " for optional reductions.");
        }
        if (!achievable) {
            suggestedActions.add("Choose a later date or lower target amount before committing.");
        }

        String recommendation = achievable
                ? "Goal appears achievable with the current sandbox financial data."
                : "Goal looks tight with the current sandbox financial data; consider a slower or smaller plan.";

        return new GoalRecommendation(
                goalName,
                necessity,
                necessityReason(necessity, goalName),
                target,
                currentSavings,
                remaining,
                targetDate,
                months,
                requiredMonthly,
                requiredWeekly,
                surplus,
                chosenMonthlySaving,
                achievable,
                estimatedCompletion,
                projectedCompletion,
                recommendation,
                keepProtected,
                reduce,
                suggestedActions,
                alternatives);
    }

    private BigDecimal parseAmount(String prompt) {
        Matcher matcher = AMOUNT_PATTERN.matcher(prompt);
        while (matcher.find()) {
            BigDecimal value = new BigDecimal(matcher.group(1).replace(",", ""));
            String suffix = matcher.group(2);
            if (suffix != null) {
                String normalized = suffix.toLowerCase(Locale.ROOT);
                if ("lakh".equals(normalized) || "lac".equals(normalized)) {
                    value = value.multiply(BigDecimal.valueOf(100000));
                } else if ("k".equals(normalized)) {
                    value = value.multiply(BigDecimal.valueOf(1000));
                }
            }
            if (value.compareTo(BigDecimal.valueOf(100)) >= 0) {
                return value;
            }
        }
        return null;
    }

    private Integer parseMonths(String prompt) {
        Matcher matcher = MONTH_PATTERN.matcher(prompt);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
    }

    private String inferGoalName(String prompt) {
        String normalized = prompt.toLowerCase(Locale.ROOT);
        for (String keyword : List.of("iphone", "laptop", "trip", "vacation", "bike", "car", "emergency fund")) {
            if (normalized.contains(keyword)) {
                return Character.toUpperCase(keyword.charAt(0)) + keyword.substring(1);
            }
        }
        return "Savings goal";
    }

    private int monthsToComplete(BigDecimal remaining, BigDecimal monthlySurplus) {
        if (monthlySurplus.signum() <= 0) return 99;
        return Math.max(1, remaining.divide(monthlySurplus, 0, RoundingMode.CEILING).intValue());
    }

    private String classifyNecessity(String goalName, String prompt) {
        String text = (goalName + " " + prompt).toLowerCase(Locale.ROOT);
        if (text.contains("medical") || text.contains("rent") || text.contains("education")) return "NECESSARY";
        if (text.contains("laptop") || text.contains("phone") || text.contains("iphone") || text.contains("work")) return "IMPORTANT";
        if (text.contains("trip") || text.contains("vacation") || text.contains("game")) return "OPTIONAL";
        return "DISCRETIONARY";
    }

    private String necessityReason(String necessity, String goalName) {
        return switch (necessity) {
            case "NECESSARY" -> goalName + " appears tied to an essential need.";
            case "IMPORTANT" -> goalName + " may support productivity, work, study, or communication.";
            case "OPTIONAL" -> goalName + " looks valuable, but can usually flex around essential obligations.";
            default -> goalName + " appears discretionary based on the current request text.";
        };
    }

    private String money(BigDecimal value) {
        return "INR " + value.setScale(2, RoundingMode.HALF_UP);
    }
}
