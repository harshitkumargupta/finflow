package com.finflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CardRequest(
        @NotBlank(message = "Cardholder name is required")
        @Size(max = 100, message = "Cardholder name must not exceed 100 characters")
        String cardholderName,

        @NotBlank(message = "Card network is required")
        @Pattern(regexp = "VISA|MASTERCARD|AMEX|DISCOVER|RUPAY", message = "Unsupported card network")
        String cardNetwork,

        @NotBlank(message = "Last four digits are required")
        @Pattern(regexp = "^[0-9]{4}$", message = "Last four digits must be exactly four digits")
        String lastFourDigits,

        @NotBlank(message = "Expiry month is required")
        @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Expiry month must be between 01 and 12")
        String expiryMonth,

        @NotBlank(message = "Expiry year is required")
        @Pattern(regexp = "^[0-9]{4}$", message = "Expiry year must be a 4-digit number")
        String expiryYear,

        @Size(max = 50, message = "Nickname must not exceed 50 characters")
        String nickname
) {
}
