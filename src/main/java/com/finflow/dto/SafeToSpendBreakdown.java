package com.finflow.dto;

import java.math.BigDecimal;

public record SafeToSpendBreakdown(String type, String description, BigDecimal amount) { }
