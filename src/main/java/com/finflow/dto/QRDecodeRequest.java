package com.finflow.dto;

import jakarta.validation.constraints.NotBlank;

public record QRDecodeRequest(
        @NotBlank(message = "QR payload is required")
        String qrPayload
) {
}
