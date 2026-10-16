package com.finflow.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.finflow.entity.SavingsGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {
    List<SavingsGoal> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<SavingsGoal> findByIdAndUserId(Long id, Long userId);

    Page<SavingsGoal> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);
}
