package com.finflow;

import com.finflow.service.BudgetProjectionCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BudgetProjectionCalculatorTest {
    private final LocalDate start = LocalDate.of(2026, 10, 1);
    private final LocalDate end = LocalDate.of(2026, 10, 31);

    @Test
    void normalBudgetIsOk() {
        var result = BudgetProjectionCalculator.calculate(new BigDecimal("100"), new BigDecimal("10"),
                start, end, LocalDate.of(2026, 10, 10));
        assertEquals("OK", result.status());
    }

    @Test
    void eightyPercentIsWarning() {
        var result = BudgetProjectionCalculator.calculate(new BigDecimal("100"), new BigDecimal("80"),
                start, end, end);
        assertEquals("WARNING_80", result.status());
    }

    @Test
    void spentBudgetIsExceeded() {
        var result = BudgetProjectionCalculator.calculate(new BigDecimal("100"), new BigDecimal("110"),
                start, end, end);
        assertEquals("EXCEEDED", result.status());
    }

    @Test
    void activeSpendCanProjectOver() {
        var result = BudgetProjectionCalculator.calculate(new BigDecimal("100"), new BigDecimal("60"),
                start, end, LocalDate.of(2026, 10, 10));
        assertEquals("PROJECTED_OVER", result.status());
    }

    @Test
    void noSpendHasNoProjection() {
        var result = BudgetProjectionCalculator.calculate(new BigDecimal("100"), BigDecimal.ZERO,
                start, end, LocalDate.of(2026, 10, 10));
        assertNull(result.projectedMonthEnd());
        assertEquals("OK", result.status());
    }
}
