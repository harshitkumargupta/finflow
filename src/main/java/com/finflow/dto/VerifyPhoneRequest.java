package com.finflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyPhoneRequest(@NotBlank @Pattern(regexp = "\\d{6}", message = "Enter the 6-digit code") String code) {
}
