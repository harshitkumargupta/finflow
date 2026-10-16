package com.finflow.dto;

import java.time.LocalDateTime;

public record CardResponse(
        Long id,
        String nickname,
        String cardType,
        String cardholderName,
        String lastFourDigits,
        String cardNetwork,
        String expiryMonth,
        String expiryYear,
        String status,
        long autopayCount,
        LocalDateTime createdAt
) {
}
