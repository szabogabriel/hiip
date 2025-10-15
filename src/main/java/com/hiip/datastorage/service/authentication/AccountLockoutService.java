package com.hiip.datastorage.service.authentication;

import com.hiip.datastorage.entity.User;
import com.hiip.datastorage.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AccountLockoutService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Value("${hiip.security.max-failed-attempts:5}")
    private int maxFailedAttempts;
    
    @Value("${hiip.security.lockout-duration-minutes:30}")
    private int lockoutDurationMinutes;
    
    /**
     * Records a failed login attempt for the user and locks the account if necessary
     */
    public void recordFailedLoginAttempt(String username) {
        userRepository.findByUsername(username)
            .ifPresent(user -> {
                user.incrementFailedLoginAttempts();
                
                if (user.getFailedLoginAttempts() >= maxFailedAttempts) {
                    lockAccount(user);
                }
                
                userRepository.save(user);
            });
    }
    
    /**
     * Records a successful login and resets failed login attempts
     */
    public void recordSuccessfulLogin(String username) {
        userRepository.findByUsername(username)
            .ifPresent(user -> {
                user.resetFailedLoginAttempts();
                userRepository.save(user);
            });
    }
    
    /**
     * Checks if a user account is currently locked
     */
    public boolean isAccountLocked(String username) {
        return userRepository.findByUsername(username)
            .map(User::isAccountLocked)
            .orElse(false);
    }
    
    /**
     * Manually unlock a user account (for admin purposes)
     */
    public boolean unlockAccount(String username) {
        return userRepository.findByUsername(username)
            .map(user -> {
                user.resetFailedLoginAttempts();
                userRepository.save(user);
                return true;
            })
            .orElse(false);
    }
    
    /**
     * Get the remaining lockout time in minutes for a user
     */
    public long getRemainingLockoutTimeMinutes(String username) {
        return userRepository.findByUsername(username)
            .map(user -> {
                if (user.getLockedUntil() == null) {
                    return 0L;
                }
                
                LocalDateTime now = LocalDateTime.now();
                if (user.getLockedUntil().isAfter(now)) {
                    return java.time.Duration.between(now, user.getLockedUntil()).toMinutes();
                }
                
                return 0L;
            })
            .orElse(0L);
    }
    
    /**
     * Get the number of failed login attempts for a user
     */
    public int getFailedLoginAttempts(String username) {
        return userRepository.findByUsername(username)
            .map(User::getFailedLoginAttempts)
            .orElse(0);
    }
    
    private void lockAccount(User user) {
        user.setLockedUntil(LocalDateTime.now().plusMinutes(lockoutDurationMinutes));
    }
}