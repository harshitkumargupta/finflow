package com.finflow.repository;

import com.finflow.entity.GoalContribution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoalContributionRepository extends JpaRepository<GoalContribution, Long> {
    List<GoalContribution> findByGoalIdAndUserIdOrderByCreatedAtDescIdDesc(Long goalId, Long userId);
    Page<GoalContribution> findByGoalIdAndUserIdOrderByCreatedAtDescIdDesc(Long goalId, Long userId, Pageable pageable);
}
