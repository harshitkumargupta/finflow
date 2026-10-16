package com.finflow.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResult(
        String paymentId,
        PaymentStatus status,
        BigDecimal amount,
        String providerReference,
        String message,
        LocalDateTime timestamp
) {}
