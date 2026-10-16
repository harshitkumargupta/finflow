package com.finflow.repository;

import com.finflow.entity.PhoneVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PhoneVerificationTokenRepository extends JpaRepository<PhoneVerificationToken, Long> {
    Optional<PhoneVerificationToken> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime since);

    @Modifying
    @Query("UPDATE PhoneVerificationToken t SET t.usedAt = :now WHERE t.user.id = :userId AND t.usedAt IS NULL")
    int invalidateOpenTokens(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
