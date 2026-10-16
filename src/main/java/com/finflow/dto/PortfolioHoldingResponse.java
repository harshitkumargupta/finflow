package com.finflow.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PortfolioHoldingResponse(
        Long id,
        String symbol,
        String name,
        String category,
        BigDecimal quantity,
        BigDecimal averagePrice,
        BigDecimal invested,
        boolean priceAvailable,
        BigDecimal marketPrice,
        BigDecimal currentValue,
        BigDecimal gainLoss,
        BigDecimal gainLossPercent,
        BigDecimal allocationPercent,
        String notes,
        LocalDateTime updatedAt) {
}
