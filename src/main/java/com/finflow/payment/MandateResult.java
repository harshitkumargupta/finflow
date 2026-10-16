package com.finflow.payment;

import java.time.LocalDateTime;

public record MandateResult(
        String mandateId,
        String providerMandateId,
        PaymentStatus status,
        String message,
        LocalDateTime timestamp
) {}
