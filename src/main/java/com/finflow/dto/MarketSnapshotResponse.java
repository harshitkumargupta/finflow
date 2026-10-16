package com.finflow.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record MarketSnapshotResponse(
        boolean available,
        String message,
        Instant asOf,
        List<MarketIndex> indices) {
    public record MarketIndex(String name, String symbol, BigDecimal value, BigDecimal changePercent) {
    }
}
