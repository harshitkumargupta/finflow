package com.finflow.dto;

public record GoalPlanningResponse(
        String provider,
        String mode,
        FinancialSnapshot snapshot,
        GoalRecommendation recommendation
) {
}
