package com.finflow.email;

public record WelcomeEmailRequested(Long userId, String to, String name, String accountNumber) {
}
