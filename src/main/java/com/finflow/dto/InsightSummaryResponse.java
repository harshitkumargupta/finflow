package com.finflow.dto;

import java.math.BigDecimal;
import java.util.List;

public record InsightSummaryResponse(boolean aiGenerated, String source, String summary, List<String> suggestions,
                                     BigDecimal monthlyIncome, BigDecimal monthlyExpenses, BigDecimal monthlySavings,
                                     BigDecimal savingsRate, BigDecimal monthOverMonthExpenseDelta,
                                     List<CategoryFigure> topCategories) {
}
