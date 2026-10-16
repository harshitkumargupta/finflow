package com.finflow.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletResponse(
        Long id,
        String walletIdentifier,
        String paymentId,
        BigDecimal totalBalance,
        BigDecimal availableBalance,
        BigDecimal reservedBalance,
        BigDecimal balance,
        String currency,
        String status,
        LocalDateTime createdAt) {
}
