package com.finflow;

import com.finflow.dto.PortfolioSummaryResponse;
import com.finflow.entity.PortfolioHolding;
import com.finflow.investment.MarketPriceProvider;
import com.finflow.service.PortfolioService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PortfolioServiceTest {

    private PortfolioHolding holding(String symbol, String qty, String avg) {
        PortfolioHolding h = new PortfolioHolding();
        h.setSymbol(symbol);
        h.setName(symbol);
        h.setCategory("STOCKS");
        h.setQuantity(new BigDecimal(qty));
        h.setAveragePrice(new BigDecimal(avg));
        return h;
    }

    private PortfolioService withPrices(Map<String, String> prices) {
        MarketPriceProvider provider = new MarketPriceProvider() {
            public String providerName() { return prices.isEmpty() ? "NONE" : "TEST"; }
            public Optional<BigDecimal> priceFor(String symbol) {
                return Optional.ofNullable(prices.get(symbol)).map(BigDecimal::new);
            }
        };
        return new PortfolioService(null, null, provider);
    }

    @Test
    void investedAndAllocationComputedWithoutPrices() {
        var service = withPrices(Map.of());
        PortfolioSummaryResponse summary = service.build(List.of(holding("ACME", "10", "100.00"), holding("ZED", "5", "60.00")));
        assertEquals(new BigDecimal("1300.00"), summary.totalInvested());
        assertFalse(summary.priceAvailable());
        assertNull(summary.totalCurrentValue());
        assertEquals("NONE", summary.priceProvider());
        var acme = summary.holdings().get(0);
        assertEquals(new BigDecimal("1000.00"), acme.invested());
        assertFalse(acme.priceAvailable());
        assertNull(acme.currentValue());
        assertEquals(0, new BigDecimal("76.92").compareTo(acme.allocationPercent()));
    }

    @Test
    void gainLossComputedWhenAllPriced() {
        var service = withPrices(Map.of("ACME", "150.00", "ZED", "40.00"));
        PortfolioSummaryResponse summary = service.build(List.of(holding("ACME", "10", "100.00"), holding("ZED", "5", "60.00")));
        assertTrue(summary.priceAvailable());
        assertEquals(new BigDecimal("1700.00"), summary.totalCurrentValue());
        assertEquals(new BigDecimal("400.00"), summary.totalGainLoss());
        var acme = summary.holdings().get(0);
        assertEquals(new BigDecimal("1500.00"), acme.currentValue());
        assertEquals(new BigDecimal("500.00"), acme.gainLoss());
        assertEquals(0, new BigDecimal("50.00").compareTo(acme.gainLossPercent()));
        var zed = summary.holdings().get(1);
        assertEquals(new BigDecimal("-100.00"), zed.gainLoss());
    }

    @Test
    void partialPricingMarksTotalUnavailable() {
        var service = withPrices(Map.of("ACME", "150.00"));
        PortfolioSummaryResponse summary = service.build(List.of(holding("ACME", "10", "100.00"), holding("ZED", "5", "60.00")));
        assertFalse(summary.priceAvailable());
        assertNull(summary.totalCurrentValue());
        assertTrue(summary.holdings().get(0).priceAvailable());
        assertFalse(summary.holdings().get(1).priceAvailable());
    }
}
