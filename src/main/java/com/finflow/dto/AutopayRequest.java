package com.finflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AutopayRequest(
        @NotNull Long accountId,
        Long cardId,
        @NotBlank @Size(max = 120) String merchantName,
        @NotBlank @Size(max = 40) String provider,
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal maximumAmount,
        @NotBlank String frequency,
        @NotNull LocalDate nextPaymentDate,
        @Size(max = 255) String description) {
}
