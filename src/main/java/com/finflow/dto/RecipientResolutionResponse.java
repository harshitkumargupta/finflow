package com.finflow.dto;

import java.math.BigDecimal;

public record RecipientResolutionResponse(
        boolean valid,
        String recipientType,
        String recipientIdentifier,
        String recipientName,
        BigDecimal suggestedAmount,
        String note,
        boolean isSandbox
) {
}
