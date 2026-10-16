package com.finflow.dto;

import java.math.BigDecimal;
import java.util.List;

public record FinancialSummaryResponse(
        BigDecimal currentBalance,
        BigDecimal safeToSpend,
        BigDecimal monthlyIncome,
        BigDecimal monthlyExpenses,
        BigDecimal monthlySavings,
        BigDecimal upcomingBills,
        BigDecimal activeAutopayAmount,
        BigDecimal goalCommitments,
        BigDecimal budgetUsage,
        BigDecimal savingsRate,
        CashFlowAnalysis cashFlow,
        List<RecurringExpense> recurringExpenses,
        List<FinancialInsight> insights,
        List<GoalFeasibility> goals) { }
