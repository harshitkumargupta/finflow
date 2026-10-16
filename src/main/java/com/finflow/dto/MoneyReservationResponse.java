package com.finflow.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MoneyReservationResponse(
        Long id,
        Long accountId,
        String bucket,
        BigDecimal amount,
        String status,
        String description,
        LocalDateTime createdAt,
        LocalDateTime releasedAt) {
}
