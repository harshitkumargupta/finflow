package com.finflow.dto;

import java.time.Instant;
import java.util.List;

public record NewsResponse(
        boolean available,
        boolean stale,
        String message,
        Instant lastUpdated,
        List<NewsArticleResponse> articles
) {
}
