package com.finflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PaymentIdentityRequest(
        @NotBlank
        @Pattern(regexp = "^[a-z0-9._-]{3,30}@finflow$",
                message = "Payment ID must be 3-30 lowercase letters, digits, dots, underscores, or hyphens followed by @finflow")
        String paymentId) {
}
