package com.finflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record PaymentExecutionRequest(
        @NotBlank(message = "Source type is required")
        String sourceType, // FINFLOW_ACCOUNT, FINFLOW_WALLET, CARD, UPI

        Long sourceId,

        @NotBlank(message = "Recipient type is required")
        String recipientType, // FINFLOW_ID, BANK_ACCOUNT, UPI_ID, QR_CODE

        @NotBlank(message = "Recipient identifier is required")
        String recipientIdentifier,

        String recipientName,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,

        String category,

        String description,

        String idempotencyKey,

        @NotBlank(message = "Transaction PIN is required")
        @Pattern(regexp = "^[0-9]{6}$", message = "Transaction PIN must be 6 digits")
        String pin
) {
}
