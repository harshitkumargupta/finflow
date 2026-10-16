package com.finflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SimulateMandateRequest(
        @NotBlank(message = "Merchant name is required")
        String merchantName,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotNull(message = "Maximum amount is required")
        @DecimalMin(value = "0.01", message = "Maximum amount must be greater than zero")
        BigDecimal maximumAmount,

        @NotBlank(message = "Frequency is required")
        String frequency, // WEEKLY, MONTHLY, QUARTERLY, YEARLY

        @NotNull(message = "Next payment date is required")
        LocalDate nextPaymentDate,

        String description,

        String provider
) {
}
