package com.finflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PortfolioHoldingRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9._-]{1,24}$") String symbol,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Pattern(regexp = "STOCKS|MUTUAL_FUNDS|INDEX_FUNDS|BONDS|FIXED_DEPOSITS|GOLD|GOVERNMENT_SECURITIES|OTHER") String category,
        @NotNull @DecimalMin("0.0001") @Digits(integer = 15, fraction = 4) BigDecimal quantity,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal averagePrice,
        @Size(max = 255) String notes) {
}
