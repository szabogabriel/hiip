package com.hiip.datastorage.service;

import org.springframework.stereotype.Service;

@Service
public class EmailService {

    public void sendPasswordResetEmail(String to, String token, String resetUrl) {
        // Placeholder for email sending logic
        System.out.println("Sending password reset email to: " + to);
        System.out.println("Token: " + token);
        System.out.println("Reset URL: " + resetUrl);
    }
}
