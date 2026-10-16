package com.finflow.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MonthlyBillLine(String name, String category, BigDecimal amount, LocalDate dueDate) {
}
