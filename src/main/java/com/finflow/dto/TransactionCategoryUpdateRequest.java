package com.finflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TransactionCategoryUpdateRequest(
        @NotBlank
        @Pattern(regexp = "FOOD|GROCERIES|SHOPPING|CLOTHING|MEDICAL|TRANSPORT|BILLS|RENT|EDUCATION|ENTERTAINMENT|INVESTMENT|TRANSFER|DEPOSIT|OTHER")
        String category
) {
}
