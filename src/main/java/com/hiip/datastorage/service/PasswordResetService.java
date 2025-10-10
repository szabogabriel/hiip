package com.hiip.datastorage.service;

import com.hiip.datastorage.entity.PasswordResetToken;
import com.hiip.datastorage.entity.User;
import com.hiip.datastorage.repository.PasswordResetTokenRepository;
import com.hiip.datastorage.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PasswordResetService {
    
    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Value("${hiip.password-reset.token-expiration-hours:24}")
    private int tokenExpirationHours;
    
    /**
     * Creates a password reset token for the given email/username
     * Returns the token string if successful, null if user not found
     */
    public String createPasswordResetToken(String usernameOrEmail) {
        Optional<User> userOpt = userRepository.findByUsername(usernameOrEmail);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(usernameOrEmail);
        }
        
        if (userOpt.isEmpty()) {
            return null; // User not found
        }
        
        User user = userOpt.get();
        
        // Check if user is active
        if (!user.getIsActive()) {
            return null; // Don't allow password reset for inactive users
        }
        
        // Delete any existing tokens for this user
        passwordResetTokenRepository.deleteByUser(user);
        
        // Create new token
        PasswordResetToken resetToken = new PasswordResetToken(user, tokenExpirationHours);
        passwordResetTokenRepository.save(resetToken);
        
        return resetToken.getToken();
    }
    
    /**
     * Validates a password reset token
     * Returns the associated user if token is valid, null otherwise
     */
    public User validatePasswordResetToken(String token) {
        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);
        
        if (tokenOpt.isEmpty()) {
            return null; // Token not found
        }
        
        PasswordResetToken resetToken = tokenOpt.get();
        
        if (!resetToken.isValid()) {
            return null; // Token expired or already used
        }
        
        return resetToken.getUser();
    }
    
    /**
     * Uses a password reset token and marks it as used
     * Returns true if successful, false if token is invalid
     */
    public boolean usePasswordResetToken(String token) {
        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);
        
        if (tokenOpt.isEmpty()) {
            return false; // Token not found
        }
        
        PasswordResetToken resetToken = tokenOpt.get();
        
        if (!resetToken.isValid()) {
            return false; // Token expired or already used
        }
        
        resetToken.markAsUsed();
        passwordResetTokenRepository.save(resetToken);
        
        return true;
    }
    
    /**
     * Checks if a user has a valid password reset token
     */
    public boolean hasValidPasswordResetToken(String usernameOrEmail) {
        Optional<User> userOpt = userRepository.findByUsername(usernameOrEmail);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(usernameOrEmail);
        }
        
        if (userOpt.isEmpty()) {
            return false;
        }
        
        return passwordResetTokenRepository.existsValidTokenForUser(userOpt.get(), LocalDateTime.now());
    }
    
    /**
     * Cleanup expired tokens (should be called periodically)
     */
    public void cleanupExpiredTokens() {
        passwordResetTokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }
    
    /**
     * Get remaining time in hours for a password reset token
     */
    public long getRemainingTimeHours(String token) {
        return passwordResetTokenRepository.findByToken(token)
            .map(resetToken -> {
                if (!resetToken.isValid()) {
                    return 0L;
                }
                
                LocalDateTime now = LocalDateTime.now();
                return java.time.Duration.between(now, resetToken.getExpiresAt()).toHours();
            })
            .orElse(0L);
    }
}