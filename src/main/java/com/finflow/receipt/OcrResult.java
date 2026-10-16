package com.finflow.receipt;

import java.util.List;

public record OcrResult(String text, List<OcrWord> words, double meanConfidence) {
    public OcrResult {
        words = words == null ? List.of() : List.copyOf(words);
    }
}
