package com.finflow.service;

import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class TransactionCategorizationService {

    public static final String CATEGORY_PATTERN = "FOOD|GROCERIES|SHOPPING|CLOTHING|MEDICAL|TRANSPORT|BILLS|RENT|EDUCATION|ENTERTAINMENT|INVESTMENT|TRANSFER|DEPOSIT|OTHER";

    private static final Set<String> SUPPORTED_CATEGORIES = Set.of(
            "FOOD",
            "GROCERIES",
            "SHOPPING",
            "CLOTHING",
            "MEDICAL",
            "TRANSPORT",
            "BILLS",
            "RENT",
            "ENTERTAINMENT",
            "EDUCATION",
            "INVESTMENT",
            "TRANSFER",
            "DEPOSIT",
            "OTHER"
    );

    private static final Map<String, String> KEYWORD_CATEGORIES = Map.ofEntries(
            Map.entry("restaurant", "FOOD"),
            Map.entry("cafe", "FOOD"),
            Map.entry("coffee", "FOOD"),
            Map.entry("food", "FOOD"),
            Map.entry("swiggy", "FOOD"),
            Map.entry("zomato", "FOOD"),
            Map.entry("grocery", "GROCERIES"),
            Map.entry("supermarket", "GROCERIES"),
            Map.entry("market", "GROCERIES"),
            Map.entry("blinkit", "GROCERIES"),
            Map.entry("zepto", "GROCERIES"),
            Map.entry("amazon", "SHOPPING"),
            Map.entry("myntra", "CLOTHING"),
            Map.entry("clothing", "CLOTHING"),
            Map.entry("apparel", "CLOTHING"),
            Map.entry("metro", "TRANSPORT"),
            Map.entry("taxi", "TRANSPORT"),
            Map.entry("cab", "TRANSPORT"),
            Map.entry("uber", "TRANSPORT"),
            Map.entry("ola", "TRANSPORT"),
            Map.entry("fuel", "TRANSPORT"),
            Map.entry("petrol", "TRANSPORT"),
            Map.entry("electricity", "BILLS"),
            Map.entry("utility", "BILLS"),
            Map.entry("rent", "RENT"),
            Map.entry("internet", "BILLS"),
            Map.entry("phone", "BILLS"),
            Map.entry("movie", "ENTERTAINMENT"),
            Map.entry("music", "ENTERTAINMENT"),
            Map.entry("game", "ENTERTAINMENT"),
            Map.entry("netflix", "ENTERTAINMENT"),
            Map.entry("spotify", "ENTERTAINMENT"),
            Map.entry("pharmacy", "MEDICAL"),
            Map.entry("apollo", "MEDICAL"),
            Map.entry("hospital", "MEDICAL"),
            Map.entry("doctor", "MEDICAL"),
            Map.entry("medical", "MEDICAL"),
            Map.entry("school", "EDUCATION"),
            Map.entry("college", "EDUCATION"),
            Map.entry("course", "EDUCATION"),
            Map.entry("book", "EDUCATION"),
            Map.entry("sip", "INVESTMENT"),
            Map.entry("mutual fund", "INVESTMENT")
    );

    public CategorySuggestion categorize(String description, String transactionType) {
        if ("DEPOSIT".equalsIgnoreCase(transactionType)) {
            return new CategorySuggestion("DEPOSIT", "HIGH", "Deposit transaction type");
        }
        if ("TRANSFER_IN".equalsIgnoreCase(transactionType)) {
            return new CategorySuggestion("TRANSFER", "HIGH", "Incoming transfer transaction type");
        }

        String normalizedDescription = normalize(description);
        for (Map.Entry<String, String> rule : KEYWORD_CATEGORIES.entrySet()) {
            if (normalizedDescription.contains(rule.getKey())) {
                return new CategorySuggestion(rule.getValue(), "HIGH", "Matched description keyword");
            }
        }

        if ("TRANSFER_OUT".equalsIgnoreCase(transactionType)) {
            return new CategorySuggestion("TRANSFER", "LOW", "No matching transfer keyword");
        }

        if ("WITHDRAWAL".equalsIgnoreCase(transactionType)) {
            return new CategorySuggestion("OTHER", "LOW", "No matching withdrawal keyword");
        }

        return new CategorySuggestion("OTHER", "LOW", "No matching category rule");
    }

    public String normalizeSupportedCategory(String category) {
        if (category == null || category.isBlank()) {
            return "OTHER";
        }
        String normalized = category.trim().toUpperCase(Locale.ROOT);
        return SUPPORTED_CATEGORIES.contains(normalized) ? normalized : "OTHER";
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    public record CategorySuggestion(
            String category,
            String confidence,
            String reason
    ) {
    }
}
