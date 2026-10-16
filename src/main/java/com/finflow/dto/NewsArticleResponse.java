package com.finflow.dto;

import java.time.Instant;

public record NewsArticleResponse(
        String title,
        String source,
        Instant publishedAt,
        String description,
        String imageUrl,
        String articleUrl,
        String category
) {
}
