package com.finflow.dto;

import java.math.BigDecimal;
import java.util.List;

public record FinancialSnapshot(
        BigDecimal accountBalance,
        BigDecimal walletBalance,
        BigDecimal currentAvailableMoney,
        BigDecimal averageMonthlyIncome,
        BigDecimal averageMonthlySpending,
        BigDecimal monthlyEssentialSpending,
        BigDecimal monthlyDiscretionarySpending,
        BigDecimal monthlyBills,
        BigDecimal monthlyAutopayObligations,
        BigDecimal existingGoalRemaining,
        List<SpendingBreakdown> spendingBreakdown
) {
}
