package com.finflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WatchlistRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9._-]{1,24}$") String symbol,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Pattern(regexp = "STOCKS|MUTUAL_FUNDS|INDEX_FUNDS|BONDS|FIXED_DEPOSITS|GOLD|GOVERNMENT_SECURITIES|OTHER")
        String category) {
}
