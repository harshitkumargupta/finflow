package com.finflow.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SavingsGoalResponse(
        Long id,
        String name,
        BigDecimal targetAmount,
        BigDecimal currentAmount,
        BigDecimal remainingAmount,
        BigDecimal percentageComplete,
        BigDecimal requiredMonthlySavings,
        BigDecimal requiredWeeklySavings,
        BigDecimal requiredDailySavings,
        Long daysRemaining,
        LocalDate estimatedCompletionDate,
        boolean onTrack,
        LocalDate targetDate,
        String description,
        String priority,
        Long accountId,
        Long activeSavingsAutopayId,
        String activeSavingsAutopayStatus,
        LocalDate nextScheduledContribution,
        String status) {
}
