package com.bitzomax.config;

import com.bitzomax.entity.User;
import com.bitzomax.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Check if an admin user already exists
        if (!userRepository.existsByUsername("max9795")) {
            User adminUser = new User();
            adminUser.setUsername("max9795");
            adminUser.setEmail("admin@bitzomax.com");
            adminUser.setPassword(passwordEncoder.encode("102067438Gerd.com"));
            adminUser.setFullName("Administrator");
            adminUser.setRole(User.Role.ADMIN);
            adminUser.setCreatedAt(LocalDateTime.now());
            adminUser.setUpdatedAt(LocalDateTime.now());
            
            userRepository.save(adminUser);
            System.out.println("Admin user created successfully");
        } else {
            System.out.println("Admin user already exists");
        }
    }
}