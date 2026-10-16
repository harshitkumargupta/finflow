package com.finflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PaymentQrScanRequest(@NotBlank @Size(max = 120) String payload) {
}
