package com.finflow.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AutopayExecutionResponse(
        Long id,
        Long autopayId,
        String merchantName,
        BigDecimal amount,
        String status,
        String failureReason,
        String transactionReference,
        boolean isSimulated,
        LocalDateTime executedAt
) {
}
