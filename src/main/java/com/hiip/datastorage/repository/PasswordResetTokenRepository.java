package com.hiip.datastorage.repository;

import com.hiip.datastorage.entity.PasswordResetToken;
import com.hiip.datastorage.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    
    /**
     * Find a password reset token by token string
     */
    Optional<PasswordResetToken> findByToken(String token);
    
    /**
     * Find a password reset token by user
     */
    Optional<PasswordResetToken> findByUser(User user);
    
    /**
     * Delete all expired tokens
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetToken prt WHERE prt.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
    
    /**
     * Delete all tokens for a specific user
     */
    @Modifying
    @Transactional
    void deleteByUser(User user);
    
    /**
     * Check if a valid (non-expired, non-used) token exists for a user
     */
    @Query("SELECT COUNT(prt) > 0 FROM PasswordResetToken prt WHERE prt.user = :user AND prt.used = false AND prt.expiresAt > :now")
    boolean existsValidTokenForUser(@Param("user") User user, @Param("now") LocalDateTime now);
}