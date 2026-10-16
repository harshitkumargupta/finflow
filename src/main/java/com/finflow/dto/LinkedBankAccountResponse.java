package com.finflow.dto;

import java.time.LocalDateTime;

public record LinkedBankAccountResponse(
        Long id,
        String bankName,
        String bankCode,
        String accountHolderName,
        String maskedAccountNumber,
        String accountType,
        String providerName,
        String providerLinkReference,
        String status,
        String verificationHint,
        String failureReason,
        boolean sandbox,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime verifiedAt,
        LocalDateTime disconnectedAt
) {
}
