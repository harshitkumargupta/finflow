package com.finflow.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record IncomeScheduleResponse(
        Long id,
        String sourceName,
        BigDecimal amount,
        String frequency,
        LocalDate nextExpectedDate,
        boolean active,
        List<LocalDate> expectedDatesWithinHorizon,
        BigDecimal expectedWithinHorizon) {
}
