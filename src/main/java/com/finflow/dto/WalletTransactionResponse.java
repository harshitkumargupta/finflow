package com.finflow.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletTransactionResponse(
        Long id,
        String paymentReference,
        String type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String currency,
        String status,
        String description,
        String counterpartyWalletIdentifier,
        Long sourceAccountId,
        LocalDateTime createdAt) {
}
