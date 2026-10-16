package com.finflow.dto;

import java.time.Instant;

public record MarketNewsArticle(
        String title,
        String source,
        Instant publishedAt,
        String category,
        String externalUrl,
        String summary) {
}
