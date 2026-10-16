package com.finflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BankLinkInitiateRequest(
        @NotBlank(message = "Bank name is required")
        @Size(max = 120, message = "Bank name must not exceed 120 characters")
        String bankName,

        @NotBlank(message = "Bank code is required")
        @Pattern(regexp = "^[A-Za-z0-9_-]{2,40}$", message = "Bank code must be 2 to 40 letters, numbers, underscores, or hyphens")
        String bankCode,

        @NotBlank(message = "Account holder name is required")
        @Size(max = 120, message = "Account holder name must not exceed 120 characters")
        String accountHolderName,

        @NotBlank(message = "Account number is required for sandbox verification")
        @Pattern(regexp = "^[0-9]{6,18}$", message = "Account number must be 6 to 18 digits")
        String accountNumber,

        @NotBlank(message = "Account type is required")
        @Pattern(regexp = "SAVINGS|CURRENT|CHECKING", message = "Account type must be SAVINGS, CURRENT, or CHECKING")
        String accountType
) {
}
