package com.finflow.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.finflow.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);
    List<Card> findByUserIdAndStatusInOrderByCreatedAtDesc(Long userId, Collection<String> statuses);
    Optional<Card> findByIdAndUserId(Long id, Long userId);

    Page<Card> findByUserIdAndStatusInOrderByCreatedAtDescIdDesc(Long userId, Collection<String> statuses, Pageable pageable);
}
