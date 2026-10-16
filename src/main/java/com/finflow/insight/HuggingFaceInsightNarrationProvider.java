package com.finflow.insight;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "finflow.insight.provider", havingValue = "HUGGINGFACE")
public class HuggingFaceInsightNarrationProvider implements InsightNarrationProvider {

    private static final Logger log = LoggerFactory.getLogger(HuggingFaceInsightNarrationProvider.class);

    private final String apiUrl;
    private final String model;
    private final String token;
    private final int maxTokens;
    private final long timeoutMs;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HuggingFaceInsightNarrationProvider(
            @Value("${finflow.insight.huggingface.api-url:https://router.huggingface.co/v1/chat/completions}") String apiUrl,
            @Value("${finflow.insight.huggingface.model:meta-llama/Llama-3.1-8B-Instruct}") String model,
            @Value("${finflow.insight.huggingface.token:}") String token,
            @Value("${finflow.insight.max-tokens:220}") int maxTokens,
            @Value("${finflow.insight.timeout-ms:8000}") long timeoutMs,
            ObjectMapper objectMapper) {
        this.apiUrl = apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;
        this.model = model;
        this.token = token;
        this.maxTokens = maxTokens;
        this.timeoutMs = timeoutMs;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(Math.min(timeoutMs, 5000))).build();
    }

    @Override
    public String name() {
        return "HUGGINGFACE";
    }

    @Override
    public boolean available() {
        return token != null && !token.isBlank();
    }

    @Override
    public String narrate(String prompt) {
        if (!available()) {
            throw new NarrationUnavailableException("Hugging Face token is not configured.");
        }
        try {
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("messages", List.of(message));
            body.put("max_tokens", maxTokens);
            body.put("temperature", 0.3);

            HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMillis(timeoutMs))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status == 429) {
                throw new NarrationUnavailableException("Hugging Face rate limit reached.");
            }
            if (status == 503) {
                throw new NarrationUnavailableException("Hugging Face model is warming up (cold start).");
            }
            if (status == 401 || status == 403) {
                throw new NarrationUnavailableException("Hugging Face token lacks Inference Providers permission.");
            }
            if (status < 200 || status >= 300) {
                throw new NarrationUnavailableException("Hugging Face returned status " + status);
            }
            String text = extractText(response.body());
            if (text == null || text.isBlank()) {
                throw new NarrationUnavailableException("Hugging Face returned an empty narration.");
            }
            return text.trim();
        } catch (NarrationUnavailableException exception) {
            log.info("Insight narration unavailable: {}", exception.getMessage());
            throw exception;
        } catch (IOException | JacksonException exception) {
            throw new NarrationUnavailableException("Hugging Face response could not be read.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new NarrationUnavailableException("Hugging Face request was interrupted.");
        }
    }

    private String extractText(String responseBody) {
        JsonNode root = objectMapper.readTree(responseBody);
        if (root.isArray() && !root.isEmpty()) {
            return root.get(0).path("generated_text").asText(null);
        }
        if (root.isObject()) {
            if (root.has("generated_text")) return root.path("generated_text").asText(null);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                return choices.get(0).path("message").path("content").asText(null);
            }
        }
        return null;
    }
}
