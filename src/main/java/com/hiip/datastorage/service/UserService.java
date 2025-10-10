package com.hiip.datastorage.service;

import com.hiip.datastorage.dto.UserRequest;
import com.hiip.datastorage.dto.UserResponse;
import com.hiip.datastorage.entity.User;
import com.hiip.datastorage.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getAllActiveUsers() {
        return userRepository.findAll().stream()
                .filter(user -> user.getIsActive())
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public Optional<UserResponse> getUserById(Long id) {
        return userRepository.findById(id)
                .map(this::convertToResponse);
    }

    public Optional<UserResponse> getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(this::convertToResponse);
    }

    public UserResponse createUser(UserRequest userRequest) {
        // Check if username already exists
        if (userRepository.findByUsername(userRequest.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists: " + userRequest.getUsername());
        }

        User user = new User();
        user.setUsername(userRequest.getUsername());
        user.setPassword(passwordEncoder.encode(userRequest.getPassword()));
        user.setEmail(userRequest.getEmail());
        user.setIsAdmin(userRequest.getIsAdmin() != null ? userRequest.getIsAdmin() : false);
        user.setActive(userRequest.getIsActive() != null ? userRequest.getIsActive() : true);

        User savedUser = userRepository.save(user);
        return convertToResponse(savedUser);
    }

    public Optional<UserResponse> updateUser(Long id, UserRequest userRequest) {
        return userRepository.findById(id)
                .map(user -> {
                    // Check if username is being changed to an existing username
                    if (!user.getUsername().equals(userRequest.getUsername()) &&
                        userRepository.findByUsername(userRequest.getUsername()).isPresent()) {
                        throw new IllegalArgumentException("Username already exists: " + userRequest.getUsername());
                    }

                    user.setUsername(userRequest.getUsername());
                    if (userRequest.getPassword() != null && !userRequest.getPassword().isEmpty()) {
                        user.setPassword(passwordEncoder.encode(userRequest.getPassword()));
                    }
                    user.setEmail(userRequest.getEmail());
                    user.setIsAdmin(userRequest.getIsAdmin() != null ? userRequest.getIsAdmin() : user.getIsAdmin());
                    user.setActive(userRequest.getIsActive() != null ? userRequest.getIsActive() : user.getIsActive());

                    User savedUser = userRepository.save(user);
                    return convertToResponse(savedUser);
                });
    }

    public boolean deactivateUser(Long id) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setActive(false);
                    userRepository.save(user);
                    return true;
                })
                .orElse(false);
    }

    public boolean activateUser(Long id) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setActive(true);
                    userRepository.save(user);
                    return true;
                })
                .orElse(false);
    }

    public boolean isAdminUser(String username) {
        return userRepository.findByUsername(username)
                .map(User::getIsAdmin)
                .orElse(false);
    }

    private UserResponse convertToResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getIsAdmin(),
                user.getIsActive()
        );
    }
}
