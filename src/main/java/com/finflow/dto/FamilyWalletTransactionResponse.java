package com.finflow.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FamilyWalletTransactionResponse(
        Long id,
        Long familyMemberId,
        String type,
        BigDecimal amount,
        String category,
        String status,
        String referenceNumber,
        String description,
        LocalDateTime createdAt) {
}
