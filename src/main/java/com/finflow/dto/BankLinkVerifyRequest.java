package com.finflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BankLinkVerifyRequest(
        @NotBlank(message = "Sandbox confirmation is required")
        @Size(max = 40, message = "Sandbox confirmation must not exceed 40 characters")
        String sandboxConfirmation
) {
}
