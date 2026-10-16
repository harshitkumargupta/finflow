package com.finflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategorizeRequest(@NotBlank @Size(max = 255) String description, @Size(max = 32) String type) {
}
