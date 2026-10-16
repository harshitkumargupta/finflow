package com.finflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record FamilySpendRequest(
        @NotNull Long accountId,
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotBlank @Pattern(regexp = "FOOD|GROCERIES|TRANSPORT|SHOPPING|BILLS|ENTERTAINMENT|HEALTH|EDUCATION|OTHER")
        String category,
        @Size(max = 160) String description,
        boolean parentApproved,
        @NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "Transaction PIN must be 6 digits") String pin) {
}
