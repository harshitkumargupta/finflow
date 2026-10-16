package com.finflow.service;

import com.finflow.dto.*;
import com.finflow.insight.InsightNarrationProvider;
import com.finflow.insight.NarrationUnavailableException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class InsightNarrationService {

    private static final List<String> OVERSPEND_STATUSES = List.of("EXCEEDED", "PROJECTED_OVER");

    private final InsightNarrationProvider provider;
    private final TransactionCategorizationService categorizationService;
    private final BudgetService budgetService;
    private final AnalyticsService analyticsService;
    private final FinancialIntelligenceService intelligenceService;

    public InsightNarrationService(InsightNarrationProvider provider,
                                   TransactionCategorizationService categorizationService,
                                   BudgetService budgetService,
                                   AnalyticsService analyticsService,
                                   FinancialIntelligenceService intelligenceService) {
        this.provider = provider;
        this.categorizationService = categorizationService;
        this.budgetService = budgetService;
        this.analyticsService = analyticsService;
        this.intelligenceService = intelligenceService;
    }

    public CategorySuggestionResponse categorize(String description, String type) {
        TransactionCategorizationService.CategorySuggestion rule = categorizationService.categorize(description, type);
        if (!"LOW".equalsIgnoreCase(rule.confidence())) {
            return new CategorySuggestionResponse(rule.category(), "RULE", true, "Matched by a FinFlow rule.");
        }
        if (!provider.available()) {
            return new CategorySuggestionResponse(rule.category(), "RULE", true, "No rule matched; defaulted to " + label(rule.category()) + ". You can change it.");
        }
        try {
            String prompt = "You classify a bank transaction for a budgeting app. Allowed categories: "
                    + TransactionCategorizationService.CATEGORY_PATTERN.replace("|", ", ")
                    + ". Reply with ONLY one category word from that list. Transaction: \"" + description + "\". Category:";
            String raw = provider.narrate(prompt);
            String suggested = matchCategory(raw);
            if (suggested == null) {
                return new CategorySuggestionResponse(rule.category(), "RULE", true, "Couldn't confidently suggest a category. You can pick one.");
            }
            return new CategorySuggestionResponse(suggested, "AI", true, "AI suggests " + label(suggested) + " based on the description. You can change it.");
        } catch (NarrationUnavailableException exception) {
            return new CategorySuggestionResponse(rule.category(), "RULE", true, "AI suggestion is unavailable right now; defaulted to " + label(rule.category()) + ".");
        }
    }

    public SpendingWarningsResponse spendingWarnings() {
        List<BudgetResponse> flagged = budgetService.list().stream()
                .filter(budget -> "ACTIVE".equals(budget.periodStatus()))
                .filter(budget -> budget.status() != null && OVERSPEND_STATUSES.contains(budget.status()))
                .sorted(Comparator.comparing((BudgetResponse budget) -> overage(budget)).reversed())
                .toList();
        List<SpendingWarningResponse> warnings = new ArrayList<>();
        boolean anyAi = false;
        for (BudgetResponse budget : flagged) {
            BigDecimal overage = overage(budget);
            String deterministic = deterministicWarning(budget, overage);
            String message = deterministic;
            boolean ai = false;
            if (provider.available()) {
                try {
                    String prompt = "Rewrite this budget alert as one short, friendly sentence for a personal finance app. "
                            + "Do not change or invent any numbers. Use only these figures. Alert: " + deterministic;
                    String narrated = provider.narrate(prompt);
                    if (narrated != null && !narrated.isBlank()) {
                        message = firstSentence(narrated);
                        ai = true;
                        anyAi = true;
                    }
                } catch (NarrationUnavailableException ignored) {
                }
            }
            warnings.add(new SpendingWarningResponse(budget.category(), budget.budgetAmount(), budget.projectedMonthEnd(), overage, budget.status(), message, ai));
        }
        String source = !provider.available() ? "NONE" : (anyAi ? "AI" : "FALLBACK");
        return new SpendingWarningsResponse(warnings, anyAi, source);
    }

    public InsightSummaryResponse summary() {
        FinancialSummaryResponse fin = intelligenceService.summary();
        List<CategoryAnalyticsResponse> categories = analyticsService.getCategoryAnalytics();
        List<MonthlyAnalyticsResponse> monthly = analyticsService.getMonthlyAnalytics();

        List<CategoryFigure> topCategories = categories.stream()
                .sorted(Comparator.comparing(CategoryAnalyticsResponse::getTotalAmount).reversed())
                .limit(3)
                .map(c -> new CategoryFigure(c.getCategory(), c.getTotalAmount(), c.getPercentage()))
                .toList();
        BigDecimal momDelta = monthOverMonthDelta(monthly);

        String deterministicSummary = deterministicSummary(fin, topCategories);
        List<String> deterministicSuggestions = deterministicSuggestions(fin, topCategories);

        if (!provider.available()) {
            return new InsightSummaryResponse(false, "NONE", deterministicSummary, deterministicSuggestions,
                    fin.monthlyIncome(), fin.monthlyExpenses(), fin.monthlySavings(), fin.savingsRate(), momDelta, topCategories);
        }
        try {
            String prompt = buildSummaryPrompt(fin, topCategories, momDelta);
            String narrated = provider.narrate(prompt);
            List<String> suggestions = extractBullets(narrated);
            String summaryText = stripBullets(narrated);
            if (summaryText.isBlank()) summaryText = deterministicSummary;
            if (suggestions.isEmpty()) suggestions = deterministicSuggestions;
            return new InsightSummaryResponse(true, "AI", summaryText, suggestions,
                    fin.monthlyIncome(), fin.monthlyExpenses(), fin.monthlySavings(), fin.savingsRate(), momDelta, topCategories);
        } catch (NarrationUnavailableException exception) {
            return new InsightSummaryResponse(false, "FALLBACK", deterministicSummary, deterministicSuggestions,
                    fin.monthlyIncome(), fin.monthlyExpenses(), fin.monthlySavings(), fin.savingsRate(), momDelta, topCategories);
        }
    }

    private String buildSummaryPrompt(FinancialSummaryResponse fin, List<CategoryFigure> top, BigDecimal momDelta) {
        StringBuilder figures = new StringBuilder();
        figures.append("Monthly income: ").append(money(fin.monthlyIncome())).append("; ");
        figures.append("monthly expenses: ").append(money(fin.monthlyExpenses())).append("; ");
        figures.append("monthly savings: ").append(money(fin.monthlySavings())).append("; ");
        figures.append("savings rate: ").append(fin.savingsRate() == null ? "n/a" : fin.savingsRate() + "%").append("; ");
        figures.append("month-over-month expense change: ").append(momDelta == null ? "n/a" : money(momDelta)).append(". ");
        figures.append("Top categories: ");
        for (CategoryFigure c : top) {
            figures.append(label(c.category())).append(" ").append(money(c.amount())).append("; ");
        }
        return "You are a personal finance assistant for a sandbox app. Using ONLY the figures below, write 2 short sentences summarising the month, "
                + "then 2 to 3 savings suggestions each on a line starting with '- '. Never invent or change numbers; use only the figures given. "
                + "Figures: " + figures;
    }

    private String matchCategory(String raw) {
        if (raw == null) return null;
        String upper = raw.toUpperCase(Locale.ROOT);
        for (String category : TransactionCategorizationService.CATEGORY_PATTERN.split("\\|")) {
            if (upper.contains(category)) {
                String normalized = categorizationService.normalizeSupportedCategory(category);
                if (!"OTHER".equals(normalized)) return normalized;
            }
        }
        return null;
    }

    private BigDecimal overage(BudgetResponse budget) {
        BigDecimal projected = budget.projectedMonthEnd() != null ? budget.projectedMonthEnd() : budget.spentAmount();
        if (projected == null || budget.budgetAmount() == null) return BigDecimal.ZERO;
        BigDecimal diff = projected.subtract(budget.budgetAmount());
        return diff.signum() > 0 ? diff.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    private String deterministicWarning(BudgetResponse budget, BigDecimal overage) {
        String category = label(budget.category());
        if ("EXCEEDED".equals(budget.status())) {
            return "You have exceeded your " + category + " budget of " + money(budget.budgetAmount()) + " by " + money(overage) + " this month.";
        }
        return "You are on pace to exceed your " + category + " budget by " + money(overage)
                + " this month (projected " + money(budget.projectedMonthEnd()) + " against " + money(budget.budgetAmount()) + ").";
    }

    private String deterministicSummary(FinancialSummaryResponse fin, List<CategoryFigure> top) {
        StringBuilder text = new StringBuilder();
        text.append("This month you earned ").append(money(fin.monthlyIncome()))
                .append(" and spent ").append(money(fin.monthlyExpenses()))
                .append(", for net savings of ").append(money(fin.monthlySavings()));
        if (fin.savingsRate() != null) text.append(" (").append(fin.savingsRate()).append("% of income)");
        text.append(".");
        if (!top.isEmpty()) {
            CategoryFigure first = top.get(0);
            text.append(" Your largest category was ").append(label(first.category())).append(" at ").append(money(first.amount())).append(".");
        }
        return text.toString();
    }

    private List<String> deterministicSuggestions(FinancialSummaryResponse fin, List<CategoryFigure> top) {
        List<String> suggestions = new ArrayList<>();
        if (!top.isEmpty()) {
            CategoryFigure first = top.get(0);
            BigDecimal tenth = first.amount().multiply(BigDecimal.valueOf(0.10)).setScale(2, RoundingMode.HALF_UP);
            suggestions.add("Trimming " + label(first.category()) + " by 10% would free up about " + money(tenth) + " a month.");
        }
        if (top.size() > 1) {
            CategoryFigure second = top.get(1);
            suggestions.add("Your second-largest category, " + label(second.category()) + " at " + money(second.amount()) + ", is another place to review.");
        }
        if (fin.recurringExpenses() != null && !fin.recurringExpenses().isEmpty()) {
            RecurringExpense recurring = fin.recurringExpenses().get(0);
            suggestions.add("You have a recurring charge of " + money(recurring.amount()) + " for " + recurring.description() + "; cancel it if it is unused.");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("Keep tracking expenses to unlock tailored savings suggestions.");
        }
        return suggestions;
    }

    private BigDecimal monthOverMonthDelta(List<MonthlyAnalyticsResponse> monthly) {
        if (monthly == null || monthly.size() < 2) return null;
        List<MonthlyAnalyticsResponse> sorted = monthly.stream()
                .sorted(Comparator.comparing(MonthlyAnalyticsResponse::getMonth))
                .toList();
        MonthlyAnalyticsResponse latest = sorted.get(sorted.size() - 1);
        MonthlyAnalyticsResponse previous = sorted.get(sorted.size() - 2);
        if (latest.getExpense() == null || previous.getExpense() == null) return null;
        return latest.getExpense().subtract(previous.getExpense()).setScale(2, RoundingMode.HALF_UP);
    }

    private List<String> extractBullets(String text) {
        List<String> bullets = new ArrayList<>();
        if (text == null) return bullets;
        for (String line : text.lines().map(String::trim).toList()) {
            if (line.startsWith("- ") || line.startsWith("• ") || line.startsWith("* ")) {
                String cleaned = line.substring(2).trim();
                if (!cleaned.isBlank()) bullets.add(cleaned);
            }
        }
        return bullets.size() > 3 ? bullets.subList(0, 3) : bullets;
    }

    private String stripBullets(String text) {
        if (text == null) return "";
        StringBuilder builder = new StringBuilder();
        for (String line : text.lines().map(String::trim).toList()) {
            if (line.isBlank() || line.startsWith("- ") || line.startsWith("• ") || line.startsWith("* ")) continue;
            builder.append(line).append(" ");
        }
        return builder.toString().trim();
    }

    private String firstSentence(String text) {
        String trimmed = text.trim();
        int end = trimmed.indexOf(". ");
        return end > 0 ? trimmed.substring(0, end + 1) : trimmed;
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
