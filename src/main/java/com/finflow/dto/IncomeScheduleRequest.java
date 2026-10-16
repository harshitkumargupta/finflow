package com.finflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IncomeScheduleRequest(
        @NotBlank @Size(max = 120) String sourceName,
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotBlank @Pattern(regexp = "ONE_TIME|WEEKLY|BIWEEKLY|MONTHLY") String frequency,
        @NotNull LocalDate nextExpectedDate,
        Boolean active) {
}
