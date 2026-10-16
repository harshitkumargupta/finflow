package com.finflow.email;

public record VerificationEmailRequested(String to, String name, String rawToken, long ttlHours) {
}
