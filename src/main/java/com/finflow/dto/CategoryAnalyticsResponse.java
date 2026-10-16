package com.finflow.dto;

import java.math.BigDecimal;

public class CategoryAnalyticsResponse {

    private final String category;
    private final BigDecimal totalAmount;
    private final long transactionCount;
    private final BigDecimal percentage;

    public CategoryAnalyticsResponse(
            String category,
            BigDecimal totalAmount,
            long transactionCount,
            BigDecimal percentage) {
        this.category = category;
        this.totalAmount = totalAmount;
        this.transactionCount = transactionCount;
        this.percentage = percentage;
    }

    public CategoryAnalyticsResponse(
            String category,
            BigDecimal totalAmount,
            long transactionCount) {
        this(category, totalAmount, transactionCount, BigDecimal.ZERO);
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public long getTransactionCount() {
        return transactionCount;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }
}
