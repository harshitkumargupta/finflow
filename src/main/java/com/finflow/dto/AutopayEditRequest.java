package com.finflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AutopayEditRequest(
        @NotBlank @Size(max = 120) String merchantName,
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal maximumAmount,
        @NotBlank String frequency,
        @NotNull LocalDate nextPaymentDate,
        Long accountId,
        Long cardId,
        @Size(max = 255) String description) {
}
