package com.finflow.service;

import com.finflow.dto.NewsArticleResponse;
import com.finflow.dto.NewsResponse;
import com.finflow.entity.User;
import com.finflow.exception.MarketProviderUnavailableException;
import com.finflow.news.NewsProvider;
import com.finflow.repository.InvestmentWatchlistRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
public class NewsService {
    private static final String UNAVAILABLE = "Live data unavailable - configure provider.";

    private final NewsProvider newsProvider;
    private final CurrentUserService currentUserService;
    private final InvestmentWatchlistRepository watchlistRepository;
    private final Duration ttl;
    private final Map<String, CachedNews> cache = new ConcurrentHashMap<>();

    public NewsService(
            NewsProvider newsProvider,
            CurrentUserService currentUserService,
            InvestmentWatchlistRepository watchlistRepository,
            @Value("${finflow.news.cache-minutes:15}") long cacheMinutes) {
        this.newsProvider = newsProvider;
        this.currentUserService = currentUserService;
        this.watchlistRepository = watchlistRepository;
        this.ttl = Duration.ofMinutes(Math.max(cacheMinutes, 1));
    }

    public NewsResponse latest() {
        return get("latest", newsProvider::topBusinessNews);
    }

    public NewsResponse markets() {
        return get("markets", newsProvider::marketNews);
    }

    public NewsResponse investments() {
        User user = currentUserService.get();
        List<String> symbols = watchlistRepository.findByUserIdOrderByAddedAtDesc(user.getId())
                .stream()
                .map(item -> item.getSymbol().toUpperCase(Locale.ROOT))
                .toList();
        return get("investments:" + String.join(",", symbols),
                () -> newsProvider.investmentNews(symbols));
    }

    public NewsResponse search(String query) {
        String safeQuery = query == null ? "" : query.trim();
        if (safeQuery.isBlank()) {
            return new NewsResponse(false, false, "Search query is required.", null, List.of());
        }
        return get("search:" + safeQuery.toLowerCase(Locale.ROOT),
                () -> newsProvider.search(safeQuery));
    }

    private NewsResponse get(String key, Supplier<List<NewsArticleResponse>> fetcher) {
        CachedNews cached = cache.get(key);
        if (cached != null && cached.fetchedAt.plus(ttl).isAfter(Instant.now())) {
            return new NewsResponse(true, false, "Live data served from recent cache.", cached.fetchedAt, cached.articles);
        }
        try {
            List<NewsArticleResponse> articles = fetcher.get();
            CachedNews updated = new CachedNews(Instant.now(), articles);
            cache.put(key, updated);
            return new NewsResponse(true, false, "Live news provided by configured backend provider.", updated.fetchedAt, articles);
        } catch (MarketProviderUnavailableException exception) {
            if (cached != null) {
                return new NewsResponse(false, true, "Live news unavailable; showing last successful data.", cached.fetchedAt, cached.articles);
            }
            return new NewsResponse(false, false, UNAVAILABLE, null, List.of());
        }
    }

    private record CachedNews(Instant fetchedAt, List<NewsArticleResponse> articles) {
    }
}
