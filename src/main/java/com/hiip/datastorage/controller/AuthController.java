package com.hiip.datastorage.controller;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
import com.hiip.datastorage.service.controller.AuthFacadeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * REST controller for authentication operations.
 * This controller handles HTTP requests and delegates business logic to AuthFacadeService.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication and user management endpoints")
public class AuthController {

    @Autowired
    private AuthFacadeService authFacadeService;

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
        Map<String, Object> result = authFacadeService.authenticateUser(
                loginRequest.getUsername(), 
                loginRequest.getPassword()
        );
        
        int httpStatus = (int) result.get("httpStatus");
        
        if ("SUCCESS".equals(result.get("status"))) {
            return ResponseEntity.ok(result.get("jwtResponse"));
        }
        
        // Remove httpStatus from response body
        result.remove("httpStatus");
        result.remove("status");
        
        return ResponseEntity.status(httpStatus).body(result);
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh JWT token",
        description = "Get a new access token using a valid refresh token"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token refreshed successfully",
            content = @Content(schema = @Schema(implementation = JwtResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid refresh token")
    })
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        Map<String, Object> result = authFacadeService.refreshToken(refreshTokenRequest.getRefreshToken());
        
        int httpStatus = (int) result.get("httpStatus");
        
        if ("SUCCESS".equals(result.get("status"))) {
            return ResponseEntity.ok(result.get("jwtResponse"));
        }
        
        return ResponseEntity.status(httpStatus).body(result.get("error"));
    }

    @PostMapping("/logout")
    @Operation(
        summary = "Logout user",
        description = "Logout the current user and revoke the JWT token"
    )
    @ApiResponse(responseCode = "200", description = "Logout successful")
    public ResponseEntity<?> logoutUser(HttpServletRequest request) {
        Map<String, Object> result = authFacadeService.logoutUser(request);
        return ResponseEntity.ok(result.get("message"));
    }

    @PostMapping("/password-reset/request")
    @Operation(
        summary = "Request password reset",
        description = "Request a password reset token for a user"
    )
    @ApiResponse(responseCode = "200", description = "Password reset request processed")
    public ResponseEntity<?> requestPasswordReset(@RequestBody PasswordResetRequest request) {
        Map<String, Object> result = authFacadeService.requestPasswordReset(request.getUsernameOrEmail());
        return ResponseEntity.ok(Map.of("message", result.get("message")));
    }

    @PostMapping("/password-reset/confirm")
    @Operation(
        summary = "Confirm password reset",
        description = "Reset password using a valid reset token"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password reset successful"),
        @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    })
    public ResponseEntity<?> confirmPasswordReset(@RequestBody PasswordResetConfirmRequest request) {
        Map<String, Object> result = authFacadeService.confirmPasswordReset(
                request.getToken(), 
                request.getNewPassword()
        );
        
        int httpStatus = (int) result.get("httpStatus");
        
        if ("SUCCESS".equals(result.get("status"))) {
            return ResponseEntity.ok(Map.of("message", result.get("message")));
        }
        
        return ResponseEntity.status(httpStatus).body(Map.of("error", result.get("error")));
    }

    @GetMapping("/password-reset/validate")
    @Operation(
        summary = "Validate password reset token",
        description = "Check if a password reset token is valid"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token validation result"),
        @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    })
    public ResponseEntity<?> validatePasswordResetToken(@RequestParam String token) {
        Map<String, Object> result = authFacadeService.validatePasswordResetToken(token);
        
        int httpStatus = (int) result.get("httpStatus");
        result.remove("httpStatus");
        
        return ResponseEntity.status(httpStatus).body(result);
    }
}
