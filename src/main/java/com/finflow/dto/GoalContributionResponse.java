package com.finflow.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GoalContributionResponse(
        Long id,
        Long goalId,
        BigDecimal amount,
        BigDecimal goalAmountAfter,
        String source,
        String reference,
        LocalDateTime createdAt) {
}
