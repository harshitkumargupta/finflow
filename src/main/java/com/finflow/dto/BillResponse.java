package com.finflow.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BillResponse(
        Long id,
        String name,
        BigDecimal amount,
        String category,
        String frequency,
        LocalDate nextDueDate,
        String description,
        Long accountId,
        String accountNumber,
        boolean reminderEnabled,
        boolean active,
        long daysUntilDue) {
}
