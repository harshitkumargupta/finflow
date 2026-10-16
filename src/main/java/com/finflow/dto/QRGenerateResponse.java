package com.finflow.dto;

import java.math.BigDecimal;

public record QRGenerateResponse(
        String qrPayload,
        String paymentId,
        String recipientName,
        BigDecimal amount,
        String description,
        boolean isSandbox
) {
}
