package com.finflow.repository;

import com.finflow.entity.InvestmentWatchlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvestmentWatchlistRepository extends JpaRepository<InvestmentWatchlistItem, Long> {
    List<InvestmentWatchlistItem> findByUserIdOrderByAddedAtDesc(Long userId);
    Optional<InvestmentWatchlistItem> findByIdAndUserId(Long id, Long userId);
    boolean existsByUserIdAndSymbol(Long userId, String symbol);
}
