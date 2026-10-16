package com.finflow.dto;

public record CategorySuggestionResponse(String category, String source, boolean overridable, String message) {
}
