package com.hiip.datastorage.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.hiip.datastorage.dto.UserRequest;
import com.hiip.datastorage.dto.UserResponse;

/**
 * Facade service for user management operations.
 * This service acts as a coordinator between the controller and business services,
 * handling authorization checks and user management operations.
 */
@Service
public class UserFacadeService {

    private static final Logger logger = LoggerFactory.getLogger(UserFacadeService.class);

    @Autowired
    private UserService userService;

    /**
     * Check if the current authenticated user has admin privileges.
     * 
     * @return true if the current user is an admin, false otherwise
     */
    public boolean isCurrentUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String username = authentication.getName();
        return userService.isAdminUser(username);
    }

    /**
     * Get all users, optionally including inactive users.
     * 
     * @param includeInactive whether to include inactive users
     * @return map containing the result of the operation
     */
    public Map<String, Object> getAllUsers(boolean includeInactive) {
        Map<String, Object> result = new HashMap<>();
        
        if (!isCurrentUserAdmin()) {
            result.put("status", "FORBIDDEN");
            result.put("error", "Access denied. Admin privileges required.");
            result.put("httpStatus", 403);
            return result;
        }

        logger.debug("Retrieving all users (includeInactive: {})", includeInactive);
        
        List<UserResponse> users = includeInactive ? 
                userService.getAllUsers() : 
                userService.getAllActiveUsers();
        
        result.put("status", "SUCCESS");
        result.put("users", users);
        result.put("httpStatus", 200);
        
        return result;
    }

    /**
     * Get user by ID.
     * 
     * @param id the user ID
     * @return map containing the result of the operation
     */
    public Map<String, Object> getUserById(Long id) {
        Map<String, Object> result = new HashMap<>();
        
        if (!isCurrentUserAdmin()) {
            result.put("status", "FORBIDDEN");
            result.put("error", "Access denied. Admin privileges required.");
            result.put("httpStatus", 403);
            return result;
        }

        logger.debug("Retrieving user with ID: {}", id);
        
        Optional<UserResponse> user = userService.getUserById(id);
        
        if (user.isPresent()) {
            result.put("status", "SUCCESS");
            result.put("user", user.get());
            result.put("httpStatus", 200);
        } else {
            result.put("status", "NOT_FOUND");
            result.put("httpStatus", 404);
        }
        
        return result;
    }

    /**
     * Get user by username.
     * 
     * @param username the username
     * @return map containing the result of the operation
     */
    public Map<String, Object> getUserByUsername(String username) {
        Map<String, Object> result = new HashMap<>();
        
        if (!isCurrentUserAdmin()) {
            result.put("status", "FORBIDDEN");
            result.put("error", "Access denied. Admin privileges required.");
            result.put("httpStatus", 403);
            return result;
        }

        logger.debug("Retrieving user with username: {}", username);
        
        Optional<UserResponse> user = userService.getUserByUsername(username);
        
        if (user.isPresent()) {
            result.put("status", "SUCCESS");
            result.put("user", user.get());
            result.put("httpStatus", 200);
        } else {
            result.put("status", "NOT_FOUND");
            result.put("httpStatus", 404);
        }
        
        return result;
    }

    /**
     * Create a new user.
     * 
     * @param userRequest the user request data
     * @return map containing the result of the operation
     */
    public Map<String, Object> createUser(UserRequest userRequest) {
        Map<String, Object> result = new HashMap<>();
        
        if (!isCurrentUserAdmin()) {
            result.put("status", "FORBIDDEN");
            result.put("error", "Access denied. Admin privileges required.");
            result.put("httpStatus", 403);
            return result;
        }

        // Basic validation
        if (userRequest.getUsername() == null || userRequest.getUsername().trim().isEmpty()) {
            result.put("status", "VALIDATION_ERROR");
            result.put("error", "Username is required");
            result.put("httpStatus", 400);
            return result;
        }
        if (userRequest.getPassword() == null || userRequest.getPassword().trim().isEmpty()) {
            result.put("status", "VALIDATION_ERROR");
            result.put("error", "Password is required");
            result.put("httpStatus", 400);
            return result;
        }
        if (userRequest.getEmail() == null || userRequest.getEmail().trim().isEmpty()) {
            result.put("status", "VALIDATION_ERROR");
            result.put("error", "Email is required");
            result.put("httpStatus", 400);
            return result;
        }

        logger.info("Creating new user: {}", userRequest.getUsername());

        try {
            UserResponse createdUser = userService.createUser(userRequest);
            result.put("status", "SUCCESS");
            result.put("user", createdUser);
            result.put("httpStatus", 201);
            
            logger.info("Successfully created user: {}", createdUser.getUsername());
            
        } catch (IllegalArgumentException e) {
            result.put("status", "VALIDATION_ERROR");
            result.put("error", e.getMessage());
            result.put("httpStatus", 400);
            
            logger.warn("Failed to create user: {}", e.getMessage());
        }
        
        return result;
    }

    /**
     * Update an existing user.
     * 
     * @param id the user ID
     * @param userRequest the user request data
     * @return map containing the result of the operation
     */
    public Map<String, Object> updateUser(Long id, UserRequest userRequest) {
        Map<String, Object> result = new HashMap<>();
        
        if (!isCurrentUserAdmin()) {
            result.put("status", "FORBIDDEN");
            result.put("error", "Access denied. Admin privileges required.");
            result.put("httpStatus", 403);
            return result;
        }

        // Basic validation
        if (userRequest.getUsername() == null || userRequest.getUsername().trim().isEmpty()) {
            result.put("status", "VALIDATION_ERROR");
            result.put("error", "Username is required");
            result.put("httpStatus", 400);
            return result;
        }
        if (userRequest.getEmail() == null || userRequest.getEmail().trim().isEmpty()) {
            result.put("status", "VALIDATION_ERROR");
            result.put("error", "Email is required");
            result.put("httpStatus", 400);
            return result;
        }

        logger.info("Updating user with ID: {}", id);

        try {
            Optional<UserResponse> updatedUser = userService.updateUser(id, userRequest);
            
            if (updatedUser.isPresent()) {
                result.put("status", "SUCCESS");
                result.put("user", updatedUser.get());
                result.put("httpStatus", 200);
                
                logger.info("Successfully updated user with ID: {}", id);
            } else {
                result.put("status", "NOT_FOUND");
                result.put("httpStatus", 404);
                
                logger.warn("User with ID {} not found", id);
            }
            
        } catch (IllegalArgumentException e) {
            result.put("status", "VALIDATION_ERROR");
            result.put("error", e.getMessage());
            result.put("httpStatus", 400);
            
            logger.warn("Failed to update user: {}", e.getMessage());
        }
        
        return result;
    }

    /**
     * Delete (deactivate) a user.
     * 
     * @param id the user ID
     * @return map containing the result of the operation
     */
    public Map<String, Object> deleteUser(Long id) {
        Map<String, Object> result = new HashMap<>();
        
        if (!isCurrentUserAdmin()) {
            result.put("status", "FORBIDDEN");
            result.put("error", "Access denied. Admin privileges required.");
            result.put("httpStatus", 403);
            return result;
        }

        logger.info("Deleting user with ID: {}", id);

        boolean deactivated = userService.deactivateUser(id);
        
        if (deactivated) {
            result.put("status", "SUCCESS");
            result.put("message", "User deactivated successfully");
            result.put("httpStatus", 200);
            
            logger.info("Successfully deactivated user with ID: {}", id);
        } else {
            result.put("status", "NOT_FOUND");
            result.put("httpStatus", 404);
            
            logger.warn("User with ID {} not found", id);
        }
        
        return result;
    }

    /**
     * Activate a user.
     * 
     * @param id the user ID
     * @return map containing the result of the operation
     */
    public Map<String, Object> activateUser(Long id) {
        Map<String, Object> result = new HashMap<>();
        
        if (!isCurrentUserAdmin()) {
            result.put("status", "FORBIDDEN");
            result.put("error", "Access denied. Admin privileges required.");
            result.put("httpStatus", 403);
            return result;
        }

        logger.info("Activating user with ID: {}", id);

        boolean activated = userService.activateUser(id);
        
        if (activated) {
            result.put("status", "SUCCESS");
            result.put("message", "User activated successfully");
            result.put("httpStatus", 200);
            
            logger.info("Successfully activated user with ID: {}", id);
        } else {
            result.put("status", "NOT_FOUND");
            result.put("httpStatus", 404);
            
            logger.warn("User with ID {} not found", id);
        }
        
        return result;
    }

    /**
     * Deactivate a user.
     * 
     * @param id the user ID
     * @return map containing the result of the operation
     */
    public Map<String, Object> deactivateUser(Long id) {
        Map<String, Object> result = new HashMap<>();
        
        if (!isCurrentUserAdmin()) {
            result.put("status", "FORBIDDEN");
            result.put("error", "Access denied. Admin privileges required.");
            result.put("httpStatus", 403);
            return result;
        }

        logger.info("Deactivating user with ID: {}", id);

        boolean deactivated = userService.deactivateUser(id);
        
        if (deactivated) {
            result.put("status", "SUCCESS");
            result.put("message", "User deactivated successfully");
            result.put("httpStatus", 200);
            
            logger.info("Successfully deactivated user with ID: {}", id);
        } else {
            result.put("status", "NOT_FOUND");
            result.put("httpStatus", 404);
            
            logger.warn("User with ID {} not found", id);
        }
        
        return result;
    }
}
