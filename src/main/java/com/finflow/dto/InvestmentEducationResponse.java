package com.finflow.dto;

import java.util.List;

public record InvestmentEducationResponse(
        String disclaimer,
        List<InvestmentCategory> categories,
        List<EducationTopic> topics) {
    public record InvestmentCategory(
            String name,
            String riskLevel,
            String typicalTimeHorizon,
            String liquidity,
            String characteristics) {
    }

    public record EducationTopic(
            String key,
            String title,
            String summary,
            String riskLevel,
            String timeHorizon) {
    }
}
