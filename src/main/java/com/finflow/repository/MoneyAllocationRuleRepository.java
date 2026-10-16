package com.finflow.repository;

import com.finflow.entity.MoneyAllocationRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MoneyAllocationRuleRepository extends JpaRepository<MoneyAllocationRule, Long> {
    List<MoneyAllocationRule> findByUserIdOrderByPriorityAscIdAsc(Long userId);
    List<MoneyAllocationRule> findByUserIdAndActiveTrueOrderByPriorityAscIdAsc(Long userId);
    Optional<MoneyAllocationRule> findByIdAndUserId(Long id, Long userId);
}
