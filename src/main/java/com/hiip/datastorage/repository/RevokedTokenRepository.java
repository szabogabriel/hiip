package com.hiip.datastorage.repository;

import com.hiip.datastorage.entity.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for managing revoked JWT tokens.
 */
@Repository
public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {

    /**
     * Check if a token exists in the revoked tokens list.
     * 
     * @param token the JWT token to check
     * @return true if the token is revoked, false otherwise
     */
    boolean existsByToken(String token);

    /**
     * Find a revoked token by its token string.
     * 
     * @param token the JWT token to find
     * @return Optional containing the revoked token if found
     */
    Optional<RevokedToken> findByToken(String token);

    /**
     * Delete all expired tokens from the blacklist.
     * This cleanup should be run periodically to prevent the table from growing indefinitely.
     * 
     * @param now the current date/time
     * @return number of deleted records
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM RevokedToken r WHERE r.expirationDate < :now")
    int deleteExpiredTokens(LocalDateTime now);
}
