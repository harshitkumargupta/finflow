package com.finflow.dto;

import java.time.LocalDateTime;

public record FamilyResponse(Long id, int memberCount, LocalDateTime createdAt) {
}
