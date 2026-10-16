package com.finflow.dto;

import java.math.BigDecimal;
import java.util.List;

public record MonthlyReportResponse(
        String month,
        String monthLabel,
        boolean hasActivity,
        BigDecimal totalIn,
        BigDecimal totalOut,
        BigDecimal net,
        long transactionCount,
        List<MonthlyCategoryLine> topCategories,
        List<MonthlyBillLine> bills,
        BigDecimal billsTotal,
        int activeAutopayCount,
        BigDecimal activeAutopayAmount,
        List<MonthlyGoalLine> goals,
        String narration,
        boolean aiGenerated,
        String narrationSource,
        String message) {
}
