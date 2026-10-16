package com.finflow.service;

import com.finflow.dto.GoalContributionResponse;
import com.finflow.dto.PageResponse;
import com.finflow.dto.PageResponseMapper;
import com.finflow.entity.GoalContribution;
import com.finflow.entity.SavingsGoal;
import com.finflow.exception.ResourceNotFoundException;
import com.finflow.repository.GoalContributionRepository;
import com.finflow.repository.SavingsGoalRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class GoalLedgerService {

    private final GoalContributionRepository contributionRepository;
    private final SavingsGoalRepository goalRepository;
    private final CurrentUserService currentUserService;

    public GoalLedgerService(GoalContributionRepository contributionRepository,
                             SavingsGoalRepository goalRepository,
                             CurrentUserService currentUserService) {
        this.contributionRepository = contributionRepository;
        this.goalRepository = goalRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public void record(SavingsGoal goal, BigDecimal amount, String source, String reference) {
        if (amount == null || amount.signum() == 0) return;
        contributionRepository.save(new GoalContribution(goal, goal.getUser(), amount, goal.getCurrentAmount(),
                source, reference, LocalDateTime.now()));
    }

    @Transactional(readOnly = true)
    public List<GoalContributionResponse> history(Long goalId) {
        Long userId = requireOwnedGoal(goalId);
        return contributionRepository.findByGoalIdAndUserIdOrderByCreatedAtDescIdDesc(goalId, userId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<GoalContributionResponse> history(Long goalId, Pageable pageable) {
        Long userId = requireOwnedGoal(goalId);
        return PageResponseMapper.map(
                contributionRepository.findByGoalIdAndUserIdOrderByCreatedAtDescIdDesc(goalId, userId, pageable),
                this::toResponse);
    }

    private Long requireOwnedGoal(Long goalId) {
        Long userId = currentUserService.get().getId();
        goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found"));
        return userId;
    }

    private GoalContributionResponse toResponse(GoalContribution contribution) {
        return new GoalContributionResponse(contribution.getId(), contribution.getGoal().getId(),
                contribution.getAmount(), contribution.getGoalAmountAfter(), contribution.getSource(),
                contribution.getReference(), contribution.getCreatedAt());
    }
}
