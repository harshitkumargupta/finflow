package com.finflow.dto;

import java.math.BigDecimal;

public record SpendingWarningResponse(String category, BigDecimal budgetAmount, BigDecimal projectedMonthEnd,
                                      BigDecimal projectedOverage, String status, String message, boolean aiGenerated) {
}
