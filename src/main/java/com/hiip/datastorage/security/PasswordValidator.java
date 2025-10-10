package com.hiip.datastorage.security;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class PasswordValidator {
    
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 128;
    
    // Common weak passwords list (in a real application, this would be loaded from a more comprehensive list)
    private static final List<String> COMMON_PASSWORDS = Arrays.asList(
        "password", "123456", "password123", "admin", "qwerty", "letmein",
        "welcome", "monkey", "1234567890", "abc123", "Password1", "password1"
    );
    
    private static final Pattern HAS_LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern HAS_UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern HAS_DIGIT = Pattern.compile("\\d");
    private static final Pattern HAS_SPECIAL_CHAR = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]");
    
    /**
     * Validates password strength and returns a list of validation errors.
     * An empty list means the password is valid.
     */
    public List<String> validatePassword(String password) {
        List<String> errors = new ArrayList<>();
        
        if (password == null || password.isEmpty()) {
            errors.add("Password cannot be empty");
            return errors;
        }
        
        // Check length requirements
        if (password.length() < MIN_PASSWORD_LENGTH) {
            errors.add("Password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
        }
        
        if (password.length() > MAX_PASSWORD_LENGTH) {
            errors.add("Password cannot exceed " + MAX_PASSWORD_LENGTH + " characters");
        }
        
        // Check character type requirements
        if (!HAS_LOWERCASE.matcher(password).find()) {
            errors.add("Password must contain at least one lowercase letter");
        }
        
        if (!HAS_UPPERCASE.matcher(password).find()) {
            errors.add("Password must contain at least one uppercase letter");
        }
        
        if (!HAS_DIGIT.matcher(password).find()) {
            errors.add("Password must contain at least one digit");
        }
        
        if (!HAS_SPECIAL_CHAR.matcher(password).find()) {
            errors.add("Password must contain at least one special character (!@#$%^&*()_+-=[]{}|;':\"\\.,<>?/)");
        }
        
        // Check against common passwords
        if (COMMON_PASSWORDS.contains(password.toLowerCase())) {
            errors.add("Password is too common and easily guessable");
        }
        
        // Check for repeated characters (more than 3 consecutive same characters)
        if (hasRepeatedCharacters(password)) {
            errors.add("Password cannot have more than 3 consecutive identical characters");
        }
        
        // Check for sequential characters (like "1234" or "abcd")
        if (hasSequentialCharacters(password)) {
            errors.add("Password cannot contain sequential characters (like '1234' or 'abcd')");
        }
        
        return errors;
    }
    
    /**
     * Returns true if password is valid (meets all requirements)
     */
    public boolean isPasswordValid(String password) {
        return validatePassword(password).isEmpty();
    }
    
    /**
     * Generates a password strength score from 0 (weakest) to 100 (strongest)
     */
    public int calculatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }
        
        int score = 0;
        
        // Length scoring (up to 25 points)
        if (password.length() >= MIN_PASSWORD_LENGTH) {
            score += Math.min(25, password.length() * 2);
        }
        
        // Character variety scoring (up to 60 points)
        if (HAS_LOWERCASE.matcher(password).find()) score += 10;
        if (HAS_UPPERCASE.matcher(password).find()) score += 10;
        if (HAS_DIGIT.matcher(password).find()) score += 10;
        if (HAS_SPECIAL_CHAR.matcher(password).find()) score += 15;
        
        // Additional complexity (up to 15 points)
        if (password.length() >= 12) score += 5;
        if (countDistinctCharacterTypes(password) >= 4) score += 5;
        if (!hasRepeatedCharacters(password)) score += 3;
        if (!hasSequentialCharacters(password)) score += 2;
        
        // Penalty for common passwords
        if (COMMON_PASSWORDS.contains(password.toLowerCase())) {
            score -= 50;
        }
        
        return Math.max(0, Math.min(100, score));
    }
    
    /**
     * Returns a human-readable description of password strength
     */
    public String getPasswordStrengthDescription(String password) {
        int strength = calculatePasswordStrength(password);
        
        if (strength < 30) return "Very Weak";
        if (strength < 50) return "Weak";
        if (strength < 70) return "Fair";
        if (strength < 85) return "Good";
        return "Strong";
    }
    
    private boolean hasRepeatedCharacters(String password) {
        int consecutiveCount = 1;
        char previousChar = 0;
        
        for (char c : password.toCharArray()) {
            if (c == previousChar) {
                consecutiveCount++;
                if (consecutiveCount > 3) {
                    return true;
                }
            } else {
                consecutiveCount = 1;
            }
            previousChar = c;
        }
        
        return false;
    }
    
    private boolean hasSequentialCharacters(String password) {
        for (int i = 0; i <= password.length() - 4; i++) {
            String substring = password.substring(i, i + 4);
            if (isSequential(substring)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isSequential(String str) {
        // Check for ascending sequence (1234, abcd)
        boolean ascending = true;
        for (int i = 1; i < str.length(); i++) {
            if (str.charAt(i) != str.charAt(i-1) + 1) {
                ascending = false;
                break;
            }
        }
        
        // Check for descending sequence (4321, dcba)
        boolean descending = true;
        for (int i = 1; i < str.length(); i++) {
            if (str.charAt(i) != str.charAt(i-1) - 1) {
                descending = false;
                break;
            }
        }
        
        return ascending || descending;
    }
    
    private int countDistinctCharacterTypes(String password) {
        int types = 0;
        if (HAS_LOWERCASE.matcher(password).find()) types++;
        if (HAS_UPPERCASE.matcher(password).find()) types++;
        if (HAS_DIGIT.matcher(password).find()) types++;
        if (HAS_SPECIAL_CHAR.matcher(password).find()) types++;
        return types;
    }
}