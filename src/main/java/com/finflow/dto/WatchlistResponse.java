package com.finflow.dto;

import java.time.LocalDateTime;

public record WatchlistResponse(
        Long id,
        String symbol,
        String name,
        String category,
        LocalDateTime addedAt,
        boolean marketDataAvailable,
        String marketDataMessage) {
}
