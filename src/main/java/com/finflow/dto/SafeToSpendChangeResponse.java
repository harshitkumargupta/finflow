package com.finflow.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SafeToSpendChangeResponse(
        boolean baselineAvailable,
        LocalDate baselineDate,
        BigDecimal previousSafeToSpend,
        BigDecimal currentSafeToSpend,
        BigDecimal change,
        String summary,
        List<SafeToSpendChangeDriver> drivers) {
}
