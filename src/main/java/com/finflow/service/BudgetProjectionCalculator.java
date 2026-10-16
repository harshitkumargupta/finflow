package com.finflow.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class BudgetProjectionCalculator {
    private BudgetProjectionCalculator() { }

    public static Result calculate(BigDecimal budgetAmount, BigDecimal spentAmount,
                                   LocalDate startDate, LocalDate endDate, LocalDate today) {
        BigDecimal spent = spentAmount == null ? BigDecimal.ZERO : spentAmount.max(BigDecimal.ZERO);
        BigDecimal percent = budgetAmount.signum() == 0 ? BigDecimal.ZERO
                : spent.multiply(BigDecimal.valueOf(100)).divide(budgetAmount, 2, RoundingMode.HALF_UP);
        boolean active = !today.isBefore(startDate) && !today.isAfter(endDate);
        BigDecimal projected = null;
        if (active && spent.signum() > 0) {
            long elapsedDays = Math.max(1, ChronoUnit.DAYS.between(startDate, today) + 1);
            long periodDays = Math.max(1, ChronoUnit.DAYS.between(startDate, endDate) + 1);
            projected = spent.multiply(BigDecimal.valueOf(periodDays))
                    .divide(BigDecimal.valueOf(elapsedDays), 2, RoundingMode.HALF_UP);
        }
        String status;
        if (spent.compareTo(budgetAmount) >= 0) status = "EXCEEDED";
        else if (projected != null && projected.compareTo(budgetAmount) > 0) status = "PROJECTED_OVER";
        else if (percent.compareTo(BigDecimal.valueOf(80)) >= 0) status = "WARNING_80";
        else status = "OK";
        return new Result(projected, percent, status);
    }

    public record Result(BigDecimal projectedMonthEnd, BigDecimal percentUsed, String status) { }
}
