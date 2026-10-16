package com.finflow.dto;

import jakarta.validation.constraints.NotBlank;

public record RecipientResolutionRequest(
        @NotBlank(message = "Recipient type is required")
        String recipientType, // FINFLOW_ID, BANK_ACCOUNT, UPI_ID, QR_CODE

        @NotBlank(message = "Identifier is required")
        String identifier
) {
}
