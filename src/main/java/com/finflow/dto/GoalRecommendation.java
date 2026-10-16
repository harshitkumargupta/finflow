package com.finflow.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record GoalRecommendation(
        String goalName,
        String necessity,
        String necessityReason,
        BigDecimal targetAmount,
        BigDecimal currentSavings,
        BigDecimal remainingAmount,
        LocalDate targetDate,
        Integer targetMonths,
        BigDecimal requiredMonthlySavings,
        BigDecimal requiredWeeklySavings,
        BigDecimal estimatedAvailableMonthlySurplus,
        BigDecimal chosenMonthlySaving,
        boolean achievable,
        LocalDate estimatedCompletionDate,
        LocalDate projectedCompletionDate,
        String recommendation,
        List<String> keepProtected,
        List<String> reduceIfNeeded,
        List<String> suggestedActions,
        List<String> alternatives
) {
}
