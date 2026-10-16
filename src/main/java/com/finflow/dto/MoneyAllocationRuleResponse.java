package com.finflow.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MoneyAllocationRuleResponse(
        Long id,
        String name,
        BigDecimal percentage,
        String destinationType,
        Long destinationId,
        int priority,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
