package com.finflow.dto;

import java.math.BigDecimal;

public record MonthlyCategoryLine(String category, BigDecimal amount, long transactionCount) {
}
