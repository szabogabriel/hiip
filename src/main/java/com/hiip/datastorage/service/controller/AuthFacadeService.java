package com.hiip.datastorage.service.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.hiip.datastorage.dto.JwtResponse;
import com.hiip.datastorage.dto.UserRequest;
import com.hiip.datastorage.entity.User;
import com.hiip.datastorage.security.JwtUtil;
import com.hiip.datastorage.service.EmailService;
import com.hiip.datastorage.service.authentication.AccountLockoutService;
import com.hiip.datastorage.service.authentication.CustomUserDetailsService;
import com.hiip.datastorage.service.authentication.PasswordResetService;
import com.hiip.datastorage.service.authentication.UserService;

/**
 * Facade service for authentication operations.
 * This service acts as a coordinator between the controller and various business services,
 * handling the orchestration of authentication, password reset, and account lockout logic.
 */
@Service
public class AuthFacadeService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private AccountLockoutService accountLockoutService;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserService userService;

    @Value("${hiip.jwt.expiration}")
    private Long jwtExpiration;

    /**
     * Authenticate user with username and password.
     * Handles account lockout logic and failed login attempts.
     * 
     * @param username the username
     * @param password the password
     * @return map containing either success data (JwtResponse) or error information
     */
    public Map<String, Object> authenticateUser(String username, String password) {
        Map<String, Object> result = new HashMap<>();
        
        // Check if account is locked
        if (accountLockoutService.isAccountLocked(username)) {
            long remainingMinutes = accountLockoutService.getRemainingLockoutTimeMinutes(username);
            result.put("status", "LOCKED");
            result.put("error", "Account is locked due to too many failed login attempts");
            result.put("remainingLockoutMinutes", remainingMinutes);
            result.put("httpStatus", 423); // 423 Locked
            return result;
        }
        
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
            
            // Record successful login (resets failed attempts)
            accountLockoutService.recordSuccessfulLogin(username);
            
            final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            final String accessToken = jwtUtil.generateToken(userDetails);
            final String refreshToken = jwtUtil.generateRefreshToken(userDetails);

            JwtResponse jwtResponse = new JwtResponse(
                    accessToken,
                    refreshToken,
                    jwtExpiration / 1000, // Convert to seconds
                    userDetails.getUsername()
            );
            
            result.put("status", "SUCCESS");
            result.put("jwtResponse", jwtResponse);
            result.put("httpStatus", 200);
            
        } catch (BadCredentialsException e) {
            // Record failed login attempt
            accountLockoutService.recordFailedLoginAttempt(username);
            
            int failedAttempts = accountLockoutService.getFailedLoginAttempts(username);
            result.put("status", "INVALID_CREDENTIALS");
            result.put("error", "Invalid credentials");
            result.put("failedAttempts", failedAttempts);
            
            // Check if account is now locked after this attempt
            if (accountLockoutService.isAccountLocked(username)) {
                long remainingMinutes = accountLockoutService.getRemainingLockoutTimeMinutes(username);
                result.put("accountLocked", true);
                result.put("remainingLockoutMinutes", remainingMinutes);
                result.put("httpStatus", 423); // 423 Locked
            } else {
                result.put("httpStatus", 401);
            }
        }
        
        return result;
    }

    /**
     * Refresh JWT tokens using a refresh token.
     * 
     * @param refreshToken the refresh token
     * @return map containing either success data (JwtResponse) or error information
     */
    public Map<String, Object> refreshToken(String refreshToken) {
        Map<String, Object> result = new HashMap<>();
        
        if (refreshToken == null || !jwtUtil.validateRefreshToken(refreshToken)) {
            result.put("status", "INVALID_TOKEN");
            result.put("error", "Invalid refresh token");
            result.put("httpStatus", 401);
            return result;
        }

        try {
            String username = jwtUtil.extractUsername(refreshToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            String newAccessToken = jwtUtil.generateToken(userDetails);
            String newRefreshToken = jwtUtil.generateRefreshToken(userDetails);

            JwtResponse jwtResponse = new JwtResponse(
                    newAccessToken,
                    newRefreshToken,
                    jwtExpiration / 1000, // Convert to seconds
                    userDetails.getUsername()
            );
            
            result.put("status", "SUCCESS");
            result.put("jwtResponse", jwtResponse);
            result.put("httpStatus", 200);
            
        } catch (Exception e) {
            result.put("status", "INVALID_TOKEN");
            result.put("error", "Invalid refresh token");
            result.put("httpStatus", 401);
        }
        
        return result;
    }

    /**
     * Request a password reset token for a user.
     * 
     * @param usernameOrEmail the username or email
     * @return map containing the result of the operation
     */
    public Map<String, Object> requestPasswordReset(String usernameOrEmail) {
        Map<String, Object> result = new HashMap<>();
        
        String token = passwordResetService.createPasswordResetToken(usernameOrEmail);
        
        if (token == null) {
            // Don't reveal whether the user exists or not for security reasons
            result.put("status", "SUCCESS");
            result.put("message", "If a user with that email/username exists, a password reset link has been sent");
            result.put("httpStatus", 200);
            return result;
        }
        
        // Send email with reset link
        emailService.sendPasswordResetEmail(
                usernameOrEmail,
                token,
                "/api/v1/auth/password-reset/confirm?token=" + token // In real app, this would be a full URL
        );
        
        result.put("status", "SUCCESS");
        result.put("message", "Password reset token generated");
        result.put("httpStatus", 200);
        
        return result;
    }

    /**
     * Confirm password reset with token and new password.
     * 
     * @param token the reset token
     * @param newPassword the new password
     * @return map containing the result of the operation
     */
    public Map<String, Object> confirmPasswordReset(String token, String newPassword) {
        Map<String, Object> result = new HashMap<>();
        
        User user = passwordResetService.validatePasswordResetToken(token);
        
        if (user == null) {
            result.put("status", "INVALID_TOKEN");
            result.put("error", "Invalid or expired password reset token");
            result.put("httpStatus", 400);
            return result;
        }
        
        try {
            // Reset the password using UserService (which will include validation)
            UserRequest userRequest = new UserRequest();
            userRequest.setUsername(user.getUsername());
            userRequest.setPassword(newPassword);
            userRequest.setEmail(user.getEmail());
            userRequest.setIsAdmin(user.getIsAdmin());
            userRequest.setIsActive(user.getIsActive());
            
            userService.updateUser(user.getId(), userRequest);
            
            // Mark token as used
            passwordResetService.usePasswordResetToken(token);
            
            // Reset any account lockout
            accountLockoutService.unlockAccount(user.getUsername());
            
            result.put("status", "SUCCESS");
            result.put("message", "Password reset successfully");
            result.put("httpStatus", 200);
            
        } catch (IllegalArgumentException e) {
            result.put("status", "VALIDATION_ERROR");
            result.put("error", e.getMessage());
            result.put("httpStatus", 400);
        }
        
        return result;
    }

    /**
     * Validate a password reset token.
     * 
     * @param token the reset token
     * @return map containing validation result
     */
    public Map<String, Object> validatePasswordResetToken(String token) {
        Map<String, Object> result = new HashMap<>();
        
        User user = passwordResetService.validatePasswordResetToken(token);
        
        if (user == null) {
            result.put("valid", false);
            result.put("error", "Invalid or expired token");
            result.put("httpStatus", 400);
            return result;
        }
        
        long remainingHours = passwordResetService.getRemainingTimeHours(token);
        
        result.put("valid", true);
        result.put("username", user.getUsername());
        result.put("remainingHours", remainingHours);
        result.put("httpStatus", 200);
        
        return result;
    }

    /**
     * Logout user.
     * In a stateless JWT system, this is handled client-side by removing the token.
     * 
     * @return map containing the result of the operation
     */
    public Map<String, Object> logoutUser() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "SUCCESS");
        result.put("message", "User logged out successfully");
        result.put("httpStatus", 200);
        return result;
    }
}
