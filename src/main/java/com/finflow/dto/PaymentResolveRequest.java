package com.finflow.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentResolveRequest(
        @NotBlank(message = "Payment ID or QR payload is required")
        String paymentId
) {
}
