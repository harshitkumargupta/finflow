package com.finflow.news;

import com.finflow.dto.NewsArticleResponse;
import com.finflow.exception.MarketProviderUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class NewsApiNewsProvider implements NewsProvider {
    private final String provider;
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public NewsApiNewsProvider(
            @Value("${finflow.news.provider:NONE}") String provider,
            @Value("${finflow.news.api-key:}") String apiKey,
            ObjectMapper objectMapper) {
        this.provider = provider;
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public List<NewsArticleResponse> topBusinessNews() {
        return fetch("https://newsapi.org/v2/top-headlines?category=business", null, "BUSINESS");
    }

    @Override
    public List<NewsArticleResponse> marketNews() {
        return fetch("https://newsapi.org/v2/everything", "markets stocks economy", "MARKETS");
    }

    @Override
    public List<NewsArticleResponse> investmentNews(List<String> symbols) {
        String query = symbols == null || symbols.isEmpty()
                ? "investing stocks mutual funds"
                : String.join(" OR ", symbols);
        return fetch("https://newsapi.org/v2/everything", query, "INVESTMENTS");
    }

    @Override
    public List<NewsArticleResponse> search(String query) {
        return fetch("https://newsapi.org/v2/everything", query, "SEARCH");
    }

    private List<NewsArticleResponse> fetch(String baseUrl, String query, String category) {
        if (!"NEWSAPI".equalsIgnoreCase(provider) || apiKey == null || apiKey.isBlank()) {
            throw new MarketProviderUnavailableException("Live news unavailable - configure NEWS_PROVIDER=NEWSAPI and NEWS_API_KEY.");
        }
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl);
            if (query != null) builder.queryParam("q", query);
            URI uri = builder
                    .queryParam("language", "en")
                    .queryParam("pageSize", 12)
                    .queryParam("sortBy", "publishedAt")
                    .encode()
                    .build()
                    .toUri();
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .header("X-Api-Key", apiKey)
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new MarketProviderUnavailableException("News provider returned status " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode articles = root.path("articles");
            List<NewsArticleResponse> result = new ArrayList<>();
            for (JsonNode article : articles) {
                if (article.path("title").asText("").isBlank()) continue;
                result.add(new NewsArticleResponse(
                        article.path("title").asText(),
                        article.path("source").path("name").asText("Unknown source"),
                        parseInstant(article.path("publishedAt").asText(null)),
                        article.path("description").asText(""),
                        article.path("urlToImage").asText(null),
                        article.path("url").asText(null),
                        category
                ));
            }
            return result;
        } catch (IOException | JacksonException exception) {
            throw new MarketProviderUnavailableException("News provider response could not be read");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new MarketProviderUnavailableException("News provider request was interrupted");
        }
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Instant.parse(value);
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
