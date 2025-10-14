package com.hiip.datastorage.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hiip.datastorage.dto.UserRequest;
import com.hiip.datastorage.dto.UserResponse;
import com.hiip.datastorage.service.UserFacadeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

/**
 * REST controller for user management operations.
 * This controller handles HTTP requests and delegates business logic to UserFacadeService.
 * All endpoints require admin privileges.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "User management endpoints (Admin only)")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    @Autowired
    private UserFacadeService userFacadeService;

    @GetMapping
    @Operation(
        summary = "Get all users",
        description = "Retrieve all users, optionally including inactive users (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin privileges required")
    })
    public ResponseEntity<?> getAllUsers(
            @RequestParam(value = "includeInactive", defaultValue = "false") boolean includeInactive) {
        
        Map<String, Object> result = userFacadeService.getAllUsers(includeInactive);
        int httpStatus = (int) result.get("httpStatus");
        
        if ("SUCCESS".equals(result.get("status"))) {
            @SuppressWarnings("unchecked")
            List<UserResponse> users = (List<UserResponse>) result.get("users");
            return ResponseEntity.ok(users);
        }
        
        return ResponseEntity.status(httpStatus).body(result.get("error"));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get user by ID",
        description = "Retrieve a specific user by ID (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User retrieved successfully",
            content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin privileges required")
    })
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        Map<String, Object> result = userFacadeService.getUserById(id);
        int httpStatus = (int) result.get("httpStatus");
        
        if ("SUCCESS".equals(result.get("status"))) {
            return ResponseEntity.ok(result.get("user"));
        }
        
        if ("NOT_FOUND".equals(result.get("status"))) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.status(httpStatus).body(result.get("error"));
    }

    @GetMapping("/username/{username}")
    @Operation(
        summary = "Get user by username",
        description = "Retrieve a specific user by username (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User retrieved successfully",
            content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin privileges required")
    })
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        Map<String, Object> result = userFacadeService.getUserByUsername(username);
        int httpStatus = (int) result.get("httpStatus");
        
        if ("SUCCESS".equals(result.get("status"))) {
            return ResponseEntity.ok(result.get("user"));
        }
        
        if ("NOT_FOUND".equals(result.get("status"))) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.status(httpStatus).body(result.get("error"));
    }

    @PostMapping
    @Operation(
        summary = "Create new user",
        description = "Create a new user (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User created successfully",
            content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request or validation error"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin privileges required")
    })
    public ResponseEntity<?> createUser(@RequestBody UserRequest userRequest) {
        Map<String, Object> result = userFacadeService.createUser(userRequest);
        int httpStatus = (int) result.get("httpStatus");
        
        if ("SUCCESS".equals(result.get("status"))) {
            return ResponseEntity.status(httpStatus).body(result.get("user"));
        }
        
        return ResponseEntity.status(httpStatus).body(result.get("error"));
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Update user",
        description = "Update an existing user (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User updated successfully",
            content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "400", description = "Invalid request or validation error"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin privileges required")
    })
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UserRequest userRequest) {
        Map<String, Object> result = userFacadeService.updateUser(id, userRequest);
        int httpStatus = (int) result.get("httpStatus");
        
        if ("SUCCESS".equals(result.get("status"))) {
            return ResponseEntity.ok(result.get("user"));
        }
        
        if ("NOT_FOUND".equals(result.get("status"))) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.status(httpStatus).body(result.get("error"));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete user",
        description = "Deactivate a user (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User deactivated successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin privileges required")
    })
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        Map<String, Object> result = userFacadeService.deleteUser(id);
        int httpStatus = (int) result.get("httpStatus");
        
        if ("SUCCESS".equals(result.get("status"))) {
            return ResponseEntity.ok().body(result.get("message"));
        }
        
        if ("NOT_FOUND".equals(result.get("status"))) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.status(httpStatus).body(result.get("error"));
    }

    @PatchMapping("/{id}/activate")
    @Operation(
        summary = "Activate user",
        description = "Activate a deactivated user (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User activated successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin privileges required")
    })
    public ResponseEntity<?> activateUser(@PathVariable Long id) {
        Map<String, Object> result = userFacadeService.activateUser(id);
        int httpStatus = (int) result.get("httpStatus");
        
        if ("SUCCESS".equals(result.get("status"))) {
            return ResponseEntity.ok().body(result.get("message"));
        }
        
        if ("NOT_FOUND".equals(result.get("status"))) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.status(httpStatus).body(result.get("error"));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(
        summary = "Deactivate user",
        description = "Deactivate an active user (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User deactivated successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin privileges required")
    })
    public ResponseEntity<?> deactivateUser(@PathVariable Long id) {
        Map<String, Object> result = userFacadeService.deactivateUser(id);
        int httpStatus = (int) result.get("httpStatus");
        
        if ("SUCCESS".equals(result.get("status"))) {
            return ResponseEntity.ok().body(result.get("message"));
        }
        
        if ("NOT_FOUND".equals(result.get("status"))) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.status(httpStatus).body(result.get("error"));
    }
}
