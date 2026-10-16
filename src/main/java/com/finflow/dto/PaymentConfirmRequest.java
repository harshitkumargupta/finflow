package com.finflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PaymentConfirmRequest(
        Long senderAccountId,
        @NotBlank @Pattern(regexp = "^[a-z0-9._-]{3,30}@finflow$")
        String receiverPaymentId,
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2)
        BigDecimal amount,
        @Pattern(regexp = "FOOD|GROCERIES|SHOPPING|CLOTHING|MEDICAL|TRANSPORT|BILLS|RENT|EDUCATION|ENTERTAINMENT|INVESTMENT|TRANSFER|DEPOSIT|OTHER")
        String category,
        @Size(max = 255) String description,
        @NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "Transaction PIN must be 6 digits")
        String pin) {
}
