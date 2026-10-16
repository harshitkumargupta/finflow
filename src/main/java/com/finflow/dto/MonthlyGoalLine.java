package com.finflow.dto;

import java.math.BigDecimal;

public record MonthlyGoalLine(String name, BigDecimal currentAmount, BigDecimal targetAmount, BigDecimal percentComplete) {
}
