package com.finflow.repository;

import com.finflow.entity.PortfolioHolding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PortfolioHoldingRepository extends JpaRepository<PortfolioHolding, Long> {
    List<PortfolioHolding> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);
    Optional<PortfolioHolding> findByIdAndUserId(Long id, Long userId);
}
