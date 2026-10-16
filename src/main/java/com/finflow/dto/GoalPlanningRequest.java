package com.finflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalPlanningRequest(
        @Size(max = 255) String prompt,
        @Size(max = 120) String goalName,
        @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal targetAmount,
        LocalDate targetDate,
        Integer targetMonths,
        @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal chosenMonthlySaving
) {
}
