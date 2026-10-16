package com.finflow.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BudgetResponse(
        Long id,
        String category,
        BigDecimal budgetAmount,
        BigDecimal spentAmount,
        BigDecimal remainingAmount,
        BigDecimal percentageUsed,
        boolean exceeded,
        LocalDate startDate,
        LocalDate endDate,
        String periodStatus,
        BigDecimal projectedMonthEnd,
        String status,
        BigDecimal percentUsed) {
}
