package com.finflow.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletPaymentResponse(
        Long id,
        String referenceNumber,
        String senderPaymentId,
        String receiverPaymentId,
        BigDecimal amount,
        String category,
        String description,
        String provider,
        String status,
        LocalDateTime createdAt) {
}
