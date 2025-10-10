package com.hiip.datastorage.controller;

import com.hiip.datastorage.dto.UserRequest;
import com.hiip.datastorage.dto.UserResponse;
import com.hiip.datastorage.entity.User;
import com.hiip.datastorage.repository.UserRepository;
import com.hiip.datastorage.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    private boolean isCurrentUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String username = authentication.getName();
        return userService.isAdminUser(username);
    }

    private ResponseEntity<?> checkAdminAccess() {
        if (!isCurrentUserAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied. Admin privileges required.");
        }
        return null;
    }

    @GetMapping
    public ResponseEntity<?> getAllUsers(@RequestParam(value = "includeInactive", defaultValue = "false") boolean includeInactive) {
        ResponseEntity<?> adminCheck = checkAdminAccess();
        if (adminCheck != null) return adminCheck;

        List<UserResponse> users = includeInactive ? 
                userService.getAllUsers() : 
                userService.getAllActiveUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        ResponseEntity<?> adminCheck = checkAdminAccess();
        if (adminCheck != null) return adminCheck;

        Optional<UserResponse> user = userService.getUserById(id);
        return user.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        ResponseEntity<?> adminCheck = checkAdminAccess();
        if (adminCheck != null) return adminCheck;

        Optional<UserResponse> user = userService.getUserByUsername(username);
        return user.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody UserRequest userRequest) {
        ResponseEntity<?> adminCheck = checkAdminAccess();
        if (adminCheck != null) return adminCheck;

        // Basic validation
        if (userRequest.getUsername() == null || userRequest.getUsername().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Username is required");
        }
        if (userRequest.getPassword() == null || userRequest.getPassword().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Password is required");
        }
        if (userRequest.getEmail() == null || userRequest.getEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Email is required");
        }

        try {
            UserResponse createdUser = userService.createUser(userRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UserRequest userRequest) {
        ResponseEntity<?> adminCheck = checkAdminAccess();
        if (adminCheck != null) return adminCheck;

        // Basic validation
        if (userRequest.getUsername() == null || userRequest.getUsername().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Username is required");
        }
        if (userRequest.getEmail() == null || userRequest.getEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Email is required");
        }

        try {
            Optional<UserResponse> updatedUser = userService.updateUser(id, userRequest);
            return updatedUser.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        ResponseEntity<?> adminCheck = checkAdminAccess();
        if (adminCheck != null) return adminCheck;

        boolean deactivated = userService.deactivateUser(id);
        if (deactivated) {
            return ResponseEntity.ok().body("User deactivated successfully");
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<?> activateUser(@PathVariable Long id) {
        ResponseEntity<?> adminCheck = checkAdminAccess();
        if (adminCheck != null) return adminCheck;

        boolean activated = userService.activateUser(id);
        if (activated) {
            return ResponseEntity.ok().body("User activated successfully");
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivateUser(@PathVariable Long id) {
        ResponseEntity<?> adminCheck = checkAdminAccess();
        if (adminCheck != null) return adminCheck;

        boolean deactivated = userService.deactivateUser(id);
        if (deactivated) {
            return ResponseEntity.ok().body("User deactivated successfully");
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
