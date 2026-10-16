package com.finflow.dto;

import java.util.List;

public record MarketNewsResponse(
        boolean available,
        String message,
        List<MarketNewsArticle> articles) {
}
