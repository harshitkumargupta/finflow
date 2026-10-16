package com.finflow.investment;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public interface MarketPriceProvider {

    String providerName();

    Optional<BigDecimal> priceFor(String symbol);

    default Map<String, BigDecimal> pricesFor(Collection<String> symbols) {
        Map<String, BigDecimal> prices = new LinkedHashMap<>();
        for (String symbol : symbols) {
            priceFor(symbol).ifPresent(price -> prices.put(symbol, price));
        }
        return prices;
    }
}
