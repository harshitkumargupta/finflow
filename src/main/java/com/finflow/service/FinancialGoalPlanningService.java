package com.finflow.service;

import com.finflow.dto.FinancialSnapshot;
import com.finflow.dto.GoalPlanningRequest;
import com.finflow.dto.GoalPlanningResponse;
import com.finflow.dto.GoalRecommendation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinancialGoalPlanningService {
    private final FinancialSnapshotService snapshotService;
    private final FinancialAssistantProvider assistantProvider;

    public FinancialGoalPlanningService(
            FinancialSnapshotService snapshotService,
            FinancialAssistantProvider assistantProvider) {
        this.snapshotService = snapshotService;
        this.assistantProvider = assistantProvider;
    }

    @Transactional(readOnly = true)
    public GoalPlanningResponse plan(GoalPlanningRequest request) {
        FinancialSnapshot snapshot = snapshotService.current();
        GoalRecommendation recommendation = assistantProvider.recommend(request, snapshot);
        return new GoalPlanningResponse(
                assistantProvider.providerName(),
                "SANDBOX_DETERMINISTIC",
                snapshot,
                recommendation);
    }
}
