package com.finflow.receipt;

public record ParsedField<T>(T value, int confidence, boolean flagged) {
    public static <T> ParsedField<T> missing() {
        return new ParsedField<>(null, 0, true);
    }
}
