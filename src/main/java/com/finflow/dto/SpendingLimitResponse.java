package com.finflow.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SpendingLimitResponse(
        Long id,
        String scopeType,
        String category,
        BigDecimal amount,
        BigDecimal spent,
        BigDecimal remaining,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
