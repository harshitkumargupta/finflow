package com.finflow.dto;

import java.math.BigDecimal;

public record SpendingBreakdown(
        String category,
        BigDecimal monthlyAmount,
        String necessity,
        boolean reducible
) {
}
