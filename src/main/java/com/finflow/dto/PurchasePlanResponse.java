package com.finflow.dto;

import java.math.BigDecimal;

public record PurchasePlanResponse(BigDecimal currentBalance, BigDecimal safeToSpendBefore,
                                   BigDecimal purchaseAmount, BigDecimal safeToSpendAfter,
                                   BigDecimal goalImpact, BigDecimal budgetImpact,
                                   String recommendation, String explanation) { }
