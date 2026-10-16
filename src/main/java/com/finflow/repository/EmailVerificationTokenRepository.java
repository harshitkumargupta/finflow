package com.finflow.repository;

import com.finflow.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    Optional<EmailVerificationToken> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime since);

    @Modifying
    @Query("UPDATE EmailVerificationToken t SET t.usedAt = :now WHERE t.user.id = :userId AND t.usedAt IS NULL")
    int invalidateOpenTokens(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
