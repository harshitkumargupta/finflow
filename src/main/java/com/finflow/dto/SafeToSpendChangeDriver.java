package com.finflow.dto;

import java.math.BigDecimal;

public record SafeToSpendChangeDriver(
        String type,
        String description,
        BigDecimal previousAmount,
        BigDecimal currentAmount,
        BigDecimal impact,
        String explanation) {
}
