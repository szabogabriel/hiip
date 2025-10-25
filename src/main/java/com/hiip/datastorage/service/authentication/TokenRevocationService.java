package com.hiip.datastorage.service.authentication;

import com.hiip.datastorage.entity.RevokedToken;
import com.hiip.datastorage.repository.RevokedTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for managing token revocation (blacklist).
 */
@Service
public class TokenRevocationService {

    private static final Logger logger = LoggerFactory.getLogger(TokenRevocationService.class);

    @Autowired
    private RevokedTokenRepository revokedTokenRepository;

    /**
     * Revoke a token by adding it to the blacklist.
     * 
     * @param token the JWT token to revoke
     * @param username the username associated with the token
     * @param expirationDate the expiration date of the token
     */
    @Transactional
    public void revokeToken(String token, String username, LocalDateTime expirationDate) {
        if (!revokedTokenRepository.existsByToken(token)) {
            RevokedToken revokedToken = new RevokedToken(token, username, expirationDate);
            revokedTokenRepository.save(revokedToken);
            logger.info("Token revoked for user: {} (expires: {})", username, expirationDate);
        } else {
            logger.debug("Token already revoked for user: {}", username);
        }
    }

    /**
     * Check if a token is revoked (in the blacklist).
     * 
     * @param token the JWT token to check
     * @return true if the token is revoked, false otherwise
     */
    public boolean isTokenRevoked(String token) {
        boolean isRevoked = revokedTokenRepository.existsByToken(token);
        logger.debug("Token revocation check: {} - {}", token.substring(0, Math.min(20, token.length())), isRevoked ? "REVOKED" : "VALID");
        return isRevoked;
    }

    /**
     * Clean up expired tokens from the blacklist.
     * This runs daily at 3 AM to prevent the database from growing indefinitely.
     */
    @Scheduled(cron = "0 0 3 * * *") // Run daily at 3 AM
    @Transactional
    public void cleanupExpiredTokens() {
        int deletedCount = revokedTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        if (deletedCount > 0) {
            System.out.println("Cleaned up " + deletedCount + " expired revoked tokens");
        }
    }
}
