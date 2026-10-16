package com.finflow.service;

import com.finflow.dto.FinancialSnapshot;
import com.finflow.dto.GoalPlanningRequest;
import com.finflow.dto.GoalRecommendation;

public interface FinancialAssistantProvider {
    String providerName();
    GoalRecommendation recommend(GoalPlanningRequest request, FinancialSnapshot snapshot);
}
