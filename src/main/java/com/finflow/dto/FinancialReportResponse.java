package com.finflow.dto;

import java.math.BigDecimal;
import java.util.List;

public record FinancialReportResponse(
        AnalyticsSummaryResponse summary,
        List<BudgetResponse> budgets,
        List<BillResponse> upcomingBills,
        List<SavingsGoalResponse> goals,
        BigDecimal monthlyObligations) {
}
