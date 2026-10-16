package com.finflow.dto;

import java.math.BigDecimal;

public record FinancialSafetyResponse(
        BigDecimal totalBalance,
        BigDecimal availableBalance,
        BigDecimal reservedBalance,
        BigDecimal monthlySpendingLimit,
        BigDecimal spentThisMonth,
        BigDecimal remainingMonthlyAllowance,
        BigDecimal safeToSpend) {
}
