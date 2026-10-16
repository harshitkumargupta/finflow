package com.finflow.dto;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioSummaryResponse(
        String priceProvider,
        boolean priceAvailable,
        BigDecimal totalInvested,
        BigDecimal totalCurrentValue,
        BigDecimal totalGainLoss,
        BigDecimal totalGainLossPercent,
        List<PortfolioHoldingResponse> holdings) {
}
