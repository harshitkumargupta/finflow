package com.finflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record WalletTransferRequest(
        @NotBlank @Size(max = 32) String receiverWalletIdentifier,
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @Size(max = 255) String description,
        @NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "Transaction PIN must be 6 digits") String pin) {
}
