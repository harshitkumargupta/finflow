package com.finflow.dto;

import java.math.BigDecimal;

public record FinancialInsight(String type, String title, String description,
                               String severity, BigDecimal supportingAmount) { }
