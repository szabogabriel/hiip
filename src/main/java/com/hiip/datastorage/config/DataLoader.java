package com.hiip.datastorage.config;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.hiip.datastorage.entity.User;
import com.hiip.datastorage.repository.UserRepository;

@Component
public class DataLoader implements CommandLineRunner {

    @Value("${hiip.admin.username:hiipa}")
    private String adminUname;

    @Value("${hiip.admin.password:hiipa}")
    private String adminPass;

    @Value("${hiip.admin.email:admin@example.com}")
    private String adminEmail;

    @Value("${hiip.admin.reset-on-startup:true}")
    private boolean resetAdminOnStartup;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (resetAdminOnStartup) {
            userRepository.findByUsername(adminUname).ifPresent(user -> {
                if (!user.getIsAdmin()) {
                    throw new IllegalStateException("Existing user with username " + adminUname + " is not an admin.");
                }
                userRepository.delete(user);
            });
        }
        if (userRepository.findByUsername(adminUname).isEmpty()) {
            User user1 = new User(adminUname, passwordEncoder.encode(adminPass), adminEmail, true);
            userRepository.save(user1);
        } 
    }
}
