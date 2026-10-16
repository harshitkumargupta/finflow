package com.finflow.dto;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String type,
        String title,
        String message,
        String severity,
        boolean read,
        LocalDateTime createdAt) {
}
