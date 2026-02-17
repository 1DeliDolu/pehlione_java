package com.pehlione.web.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.pehlione.web.user.User;
import com.pehlione.web.user.UserRepository;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String email = "user@pehlione.com";

            if (!userRepository.existsByEmail(email)) {
                User u = new User();
                u.setEmail(email);
                u.setPasswordHash(passwordEncoder.encode("D0cker!"));
                u.setRoles("ROLE_USER");
                u.setEnabled(true);

                userRepository.save(u);
                System.out.println("Seed user created: " + email + " / D0cker!");

            }
        };
    }
}
