package com.finflow.repository;

import com.finflow.entity.FinanceNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FinanceNotificationRepository extends JpaRepository<FinanceNotification, Long> {
    List<FinanceNotification> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<FinanceNotification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);
    Page<FinanceNotification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<FinanceNotification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Optional<FinanceNotification> findByIdAndUserId(Long id, Long userId);
    boolean existsByUserIdAndEventKey(Long userId, String eventKey);
    long countByUserIdAndReadFalse(Long userId);

    @Modifying
    @Query(value = """
            INSERT INTO finance_notifications
                (user_id, event_key, type, title, message, severity, is_read, created_at)
            VALUES
                (:userId, :eventKey, :type, :title, :message, :severity, false, CURRENT_TIMESTAMP)
            ON CONFLICT (user_id, event_key) DO NOTHING
            """, nativeQuery = true)
    int createIfAbsent(
            @Param("userId") Long userId,
            @Param("eventKey") String eventKey,
            @Param("type") String type,
            @Param("title") String title,
            @Param("message") String message,
            @Param("severity") String severity);
}
