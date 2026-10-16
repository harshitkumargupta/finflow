package com.finflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record PaymentWebhookRequest(
        @NotBlank String eventId,
        @NotBlank String eventType,
        @NotBlank String paymentId,
        @NotBlank String status,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotNull @Positive Long timestamp) {
}
