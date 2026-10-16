package com.finflow.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SafeToSpendResponse(
        BigDecimal currentBalance,
        BigDecimal safeToSpend,
        BigDecimal safePerDay,
        LocalDate calculatedThrough,
        List<SafeToSpendBreakdown> breakdown) { }
