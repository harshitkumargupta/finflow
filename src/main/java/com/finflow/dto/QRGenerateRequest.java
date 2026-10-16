package com.finflow.dto;

import java.math.BigDecimal;

public record QRGenerateRequest(
        BigDecimal amount,
        String description,
        String category
) {
}
