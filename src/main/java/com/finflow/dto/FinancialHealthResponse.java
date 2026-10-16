package com.finflow.dto;

import java.util.List;

public record FinancialHealthResponse(int score, String rating, List<HealthFactorResponse> factors) {
}
