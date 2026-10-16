package com.finflow.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MoneyAllocationRuleRequest(
        @NotBlank @Size(max = 80) String name,
        @NotNull @DecimalMin("0.01") @DecimalMax("100.00")
        @Digits(integer = 3, fraction = 2) BigDecimal percentage,
        @NotBlank String destinationType,
        Long destinationId,
        @PositiveOrZero int priority,
        Boolean active) {
}
