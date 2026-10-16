package com.finflow.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentDomainResponse(
        Long id,
        String referenceNumber,
        String externalProviderReference,
        String sourceType,
        Long sourceId,
        String recipientType,
        String recipientIdentifier,
        String recipientName,
        BigDecimal amount,
        String currency,
        String status,
        String category,
        String description,
        String failureReason,
        String idempotencyKey,
        boolean isSandbox,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
}
