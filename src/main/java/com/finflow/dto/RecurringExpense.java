package com.finflow.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringExpense(String description, String category, BigDecimal amount,
                               String frequency, int occurrences, BigDecimal confidence,
                               LocalDate lastSeen, String reason) { }
