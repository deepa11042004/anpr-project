package com.anpr.config;

import com.anpr.entity.Role;
import com.anpr.entity.User;
import com.anpr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Create default users if they don't exist
        createUserIfNotExists("superadmin", "SuperAdmin@123", "Super Administrator", 
                "superadmin@anpr.local", Role.SUPER_ADMIN);
        createUserIfNotExists("admin", "Admin@123", "Administrator", 
                "admin@anpr.local", Role.ADMIN);
        createUserIfNotExists("operator", "Operator@123", "Gate Operator", 
                "operator@anpr.local", Role.OPERATOR);

        log.info("Default users initialized");
    }

    private void createUserIfNotExists(String username, String password, String fullName, 
                                        String email, Role role) {
        if (!userRepository.existsByUsername(username)) {
            User user = User.builder()
                    .username(username)
                    .password(passwordEncoder.encode(password))
                    .fullName(fullName)
                    .email(email)
                    .role(role)
                    .enabled(true)
                    .build();
            userRepository.save(user);
            log.info("Created default user: {} with role: {}", username, role);
        }
    }
}
