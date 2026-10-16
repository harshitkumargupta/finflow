package com.finflow.dto;

import java.math.BigDecimal;

public record CashFlowAnalysis(
        BigDecimal averageMonthlyIncome,
        BigDecimal averageMonthlyExpenses,
        BigDecimal currentMonthIncome,
        BigDecimal currentMonthExpenses,
        BigDecimal savingsRate,
        String highestSpendingCategory,
        BigDecimal monthOverMonthExpenseChange,
        boolean sufficientHistory) { }
