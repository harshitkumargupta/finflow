package com.finflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record FamilyMemberRequest(
        @NotBlank @Size(max = 60) String displayName,
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2)
        BigDecimal monthlyAllowance,
        boolean approvalRequired) {
}
