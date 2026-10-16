package com.finflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record WalletWithdrawalRequest(
        @NotNull(message = "Destination account is required")
        Long accountId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,

        String description,
        @NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "Transaction PIN must be 6 digits") String pin
) {
}
