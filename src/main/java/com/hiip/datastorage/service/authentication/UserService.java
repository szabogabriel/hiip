package com.hiip.datastorage.service.authentication;

import com.hiip.datastorage.dto.UserRequest;
import com.hiip.datastorage.dto.UserResponse;
import com.hiip.datastorage.entity.PasswordHistory;
import com.hiip.datastorage.entity.User;
import com.hiip.datastorage.repository.PasswordHistoryRepository;
import com.hiip.datastorage.repository.UserRepository;
import com.hiip.datastorage.security.PasswordValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordHistoryRepository passwordHistoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordValidator passwordValidator;

    @Value("${hiip.security.password-history-count:5}")
    private int passwordHistoryCount;

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

        // Validate password strength
        List<String> passwordErrors = passwordValidator.validatePassword(userRequest.getPassword());
        if (!passwordErrors.isEmpty()) {
            throw new IllegalArgumentException("Password validation failed: " + String.join(", ", passwordErrors));
        }

        User user = new User();
        user.setUsername(userRequest.getUsername());
        String encodedPassword = passwordEncoder.encode(userRequest.getPassword());
        user.setPassword(encodedPassword);
        user.setEmail(userRequest.getEmail());
        user.setIsAdmin(userRequest.getIsAdmin() != null ? userRequest.getIsAdmin() : false);
        user.setActive(userRequest.getIsActive() != null ? userRequest.getIsActive() : true);

        User savedUser = userRepository.save(user);
        
        // Save password to history
        savePasswordToHistory(savedUser, encodedPassword);
        
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
                    
                    // Handle password update
                    if (userRequest.getPassword() != null && !userRequest.getPassword().isEmpty()) {
                        // Validate password strength
                        List<String> passwordErrors = passwordValidator.validatePassword(userRequest.getPassword());
                        if (!passwordErrors.isEmpty()) {
                            throw new IllegalArgumentException("Password validation failed: " + String.join(", ", passwordErrors));
                        }
                        
                        // Check password history to prevent reuse (using plain password)
                        if (isPasswordInHistory(user, userRequest.getPassword())) {
                            throw new IllegalArgumentException("Password cannot be reused. Please choose a different password.");
                        }
                        
                        String newEncodedPassword = passwordEncoder.encode(userRequest.getPassword());
                        user.setPassword(newEncodedPassword);
                        
                        // Save new password to history
                        savePasswordToHistory(user, newEncodedPassword);
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

    /**
     * Save a password to the user's password history
     */
    private void savePasswordToHistory(User user, String encodedPassword) {
        PasswordHistory passwordHistory = new PasswordHistory(user, encodedPassword);
        passwordHistoryRepository.save(passwordHistory);
        
        // Clean up old password history entries, keeping only the most recent N passwords
        cleanupPasswordHistory(user);
    }

    /**
     * Check if a password is already in the user's password history
     * Note: This method requires the plain text password for comparison since BCrypt hashes are one-way
     */
    private boolean isPasswordInHistory(User user, String plainPassword) {
        List<PasswordHistory> recentPasswords = passwordHistoryRepository
                .findTopNByUserOrderByCreatedAtDesc(user, PageRequest.of(0, passwordHistoryCount));
        
        return recentPasswords.stream()
                .anyMatch(ph -> passwordEncoder.matches(plainPassword, ph.getPasswordHash()));
    }

    /**
     * Clean up old password history entries, keeping only the most recent entries
     */
    private void cleanupPasswordHistory(User user) {
        long totalCount = passwordHistoryRepository.countByUser(user);
        if (totalCount > passwordHistoryCount) {
            List<PasswordHistory> allHistory = passwordHistoryRepository.findByUserOrderByCreatedAtDesc(user);
            List<PasswordHistory> toDelete = allHistory.subList(passwordHistoryCount, allHistory.size());
            passwordHistoryRepository.deleteAll(toDelete);
        }
    }

    /**
     * Get password strength information for a password
     */
    public Map<String, Object> getPasswordStrength(String password) {
        Map<String, Object> result = new HashMap<>();
        result.put("score", passwordValidator.calculatePasswordStrength(password));
        result.put("description", passwordValidator.getPasswordStrengthDescription(password));
        result.put("validationErrors", passwordValidator.validatePassword(password));
        result.put("isValid", passwordValidator.isPasswordValid(password));
        return result;
    }
}
