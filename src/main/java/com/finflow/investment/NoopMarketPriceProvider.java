package com.finflow.investment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "finflow.market.price-provider", havingValue = "NONE", matchIfMissing = true)
public class NoopMarketPriceProvider implements MarketPriceProvider {

    @Override
    public String providerName() {
        return "NONE";
    }

    @Override
    public Optional<BigDecimal> priceFor(String symbol) {
        return Optional.empty();
    }
}
