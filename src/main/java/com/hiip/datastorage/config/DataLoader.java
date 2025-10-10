package com.hiip.datastorage.config;

import com.hiip.datastorage.entity.User;
import com.hiip.datastorage.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByUsername("user1").isEmpty()) {
            User user1 = new User("user1", passwordEncoder.encode("password1"), "user1@example.com");
            userRepository.save(user1);
        }

        if (userRepository.findByUsername("user2").isEmpty()) {
            User user2 = new User("user2", passwordEncoder.encode("password2"), "user2@example.com");
            userRepository.save(user2);
        }
    }
}
