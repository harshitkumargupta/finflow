package com.finflow.dto;

public record HealthFactorResponse(String name, int score, int maximum, String explanation) {
}
