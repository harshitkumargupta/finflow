package com.finflow.payment;

import java.math.BigDecimal;

public record PaymentCommand(
        Long senderAccountId,
        String receiverAccountNumber,
        BigDecimal amount,
        String category,
        String description,
        String idempotencyKey) {
}
