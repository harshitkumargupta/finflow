package com.finflow.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AutopayAuthorizationResponse(
        Long id,
        String merchantName,
        String provider,
        BigDecimal amount,
        BigDecimal maximumAmount,
        String frequency,
        LocalDate nextPaymentDate,
        String status,
        String description,
        String providerAuthorizationReference,
        Long accountId,
        String accountName,
        Long cardId,
        String cardNickname,
        String cardLastFourDigits,
        String cardNetwork,
        Long savingsGoalId,
        String savingsGoalName,
        LocalDate lastPaymentDate,
        String lastPaymentStatus,
        boolean isSimulated,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime cancelledAt) {
}
