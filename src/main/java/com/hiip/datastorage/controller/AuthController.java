package com.hiip.datastorage.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hiip.datastorage.dto.JwtResponse;
import com.hiip.datastorage.dto.LoginRequest;
import com.hiip.datastorage.dto.PasswordResetConfirmRequest;
import com.hiip.datastorage.dto.PasswordResetRequest;
import com.hiip.datastorage.dto.RefreshTokenRequest;
import com.hiip.datastorage.dto.UserRequest;
import com.hiip.datastorage.entity.User;
import com.hiip.datastorage.security.JwtUtil;
import com.hiip.datastorage.service.AccountLockoutService;
import com.hiip.datastorage.service.CustomUserDetailsService;
import com.hiip.datastorage.service.EmailService;
import com.hiip.datastorage.service.PasswordResetService;
import com.hiip.datastorage.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication and user management endpoints")
public class AuthController {

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

    @PostMapping("/login")
    @Operation(
        summary = "Authenticate user",
        description = "Authenticate user with username and password, returns JWT access and refresh tokens"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authentication successful",
            content = @Content(schema = @Schema(implementation = JwtResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @ApiResponse(responseCode = "423", description = "Account locked due to too many failed attempts")
    })
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        String username = loginRequest.getUsername();
        
        // Check if account is locked
        if (accountLockoutService.isAccountLocked(username)) {
            long remainingMinutes = accountLockoutService.getRemainingLockoutTimeMinutes(username);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Account is locked due to too many failed login attempts");
            response.put("remainingLockoutMinutes", remainingMinutes);
            return ResponseEntity.status(423).body(response); // 423 Locked
        }
        
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            username,
                            loginRequest.getPassword())
            );
            
            // Record successful login (resets failed attempts)
            accountLockoutService.recordSuccessfulLogin(username);
            
        } catch (BadCredentialsException e) {
            // Record failed login attempt
            accountLockoutService.recordFailedLoginAttempt(username);
            
            int failedAttempts = accountLockoutService.getFailedLoginAttempts(username);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Invalid credentials");
            response.put("failedAttempts", failedAttempts);
            
            // Check if account is now locked after this attempt
            if (accountLockoutService.isAccountLocked(username)) {
                long remainingMinutes = accountLockoutService.getRemainingLockoutTimeMinutes(username);
                response.put("accountLocked", true);
                response.put("remainingLockoutMinutes", remainingMinutes);
                return ResponseEntity.status(423).body(response); // 423 Locked
            }
            
            return ResponseEntity.status(401).body(response);
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        final String accessToken = jwtUtil.generateToken(userDetails);
        final String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        return ResponseEntity.ok(new JwtResponse(
                accessToken,
                refreshToken,
                jwtExpiration / 1000, // Convert to seconds
                userDetails.getUsername()
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        String refreshToken = refreshTokenRequest.getRefreshToken();
        
        if (refreshToken == null || !jwtUtil.validateRefreshToken(refreshToken)) {
            return ResponseEntity.status(401).body("Invalid refresh token");
        }

        try {
            String username = jwtUtil.extractUsername(refreshToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            String newAccessToken = jwtUtil.generateToken(userDetails);
            String newRefreshToken = jwtUtil.generateRefreshToken(userDetails);

            return ResponseEntity.ok(new JwtResponse(
                    newAccessToken,
                    newRefreshToken,
                    jwtExpiration / 1000, // Convert to seconds
                    userDetails.getUsername()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid refresh token");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        // In a real application, you would invalidate the token here
        // For now, we'll just return a success message
        // Token invalidation would require a token blacklist or database storage
        return ResponseEntity.ok("User logged out successfully");
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<?> requestPasswordReset(@RequestBody PasswordResetRequest request) {
        String token = passwordResetService.createPasswordResetToken(request.getUsernameOrEmail());
        
        if (token == null) {
            // Don't reveal whether the user exists or not for security reasons
            return ResponseEntity.ok(Map.of("message", "If a user with that email/username exists, a password reset link has been sent"));
        }
        
        // In a real application, you would send an email with the reset link here
        // For development/testing purposes, we return the token
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Password reset token generated");
        
        emailService.sendPasswordResetEmail(
                request.getUsernameOrEmail(),
                token,
                "/api/v1/auth/password-reset/confirm?token=" + token // In real app, this would be a full URL
        );
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<?> confirmPasswordReset(@RequestBody PasswordResetConfirmRequest request) {
        User user = passwordResetService.validatePasswordResetToken(request.getToken());
        
        if (user == null) {
            return ResponseEntity.status(400).body(Map.of("error", "Invalid or expired password reset token"));
        }
        
        try {
            // Reset the password using UserService (which will include validation)
            UserRequest userRequest = new UserRequest();
            userRequest.setUsername(user.getUsername());
            userRequest.setPassword(request.getNewPassword());
            userRequest.setEmail(user.getEmail());
            userRequest.setIsAdmin(user.getIsAdmin());
            userRequest.setIsActive(user.getIsActive());
            
            userService.updateUser(user.getId(), userRequest);
            
            // Mark token as used
            passwordResetService.usePasswordResetToken(request.getToken());
            
            // Reset any account lockout
            accountLockoutService.unlockAccount(user.getUsername());
            
            return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/password-reset/validate")
    public ResponseEntity<?> validatePasswordResetToken(@RequestParam String token) {
        User user = passwordResetService.validatePasswordResetToken(token);
        
        if (user == null) {
            return ResponseEntity.status(400).body(Map.of("valid", false, "error", "Invalid or expired token"));
        }
        
        long remainingHours = passwordResetService.getRemainingTimeHours(token);
        
        Map<String, Object> response = new HashMap<>();
        response.put("valid", true);
        response.put("username", user.getUsername());
        response.put("remainingHours", remainingHours);
        
        return ResponseEntity.ok(response);
    }
}
