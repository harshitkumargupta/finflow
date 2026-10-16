package com.finflow.dto;

import java.math.BigDecimal;

public record FamilyCategoryLimitResponse(
        Long id,
        String category,
        BigDecimal monthlyAmount,
        BigDecimal spentThisMonth,
        BigDecimal remainingThisMonth) {
}
