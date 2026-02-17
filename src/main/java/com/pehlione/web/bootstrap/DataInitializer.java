package com.pehlione.web.bootstrap;

import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.pehlione.web.user.Role;
import com.pehlione.web.user.RoleRepository;
import com.pehlione.web.user.User;
import com.pehlione.web.user.UserRepository;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedUsers(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            Role roleUser = roleRepository.findByName("ROLE_USER")
                    .orElseGet(() -> {
                        Role r = new Role();
                        r.setName("ROLE_USER");
                        return roleRepository.save(r);
                    });

            Role roleAdmin = roleRepository.findByName("ROLE_ADMIN")
                    .orElseGet(() -> {
                        Role r = new Role();
                        r.setName("ROLE_ADMIN");
                        return roleRepository.save(r);
                    });

            ensureUser(userRepository, passwordEncoder, "user@pehlione.com", "password", Set.of(roleUser));
            ensureUser(userRepository, passwordEncoder, "admin@pehlione.com", "admin123", Set.of(roleUser, roleAdmin));
        };
    }

    private void ensureUser(
            UserRepository userRepository,
            PasswordEncoder encoder,
            String email,
            String rawPassword,
            Set<Role> desiredRoles) {
        User u = userRepository.findWithRolesByEmail(email).orElseGet(User::new);

        boolean isNew = (u.getId() == null);

        u.setEmail(email);
        u.setEnabled(true);

        if (isNew) {
            u.setPasswordHash(encoder.encode(rawPassword));
        }

        if (u.getRoles() == null || u.getRoles().isEmpty()) {
            u.setRoles(new HashSet<>(desiredRoles));
        } else {
            u.getRoles().addAll(desiredRoles);
        }

        userRepository.save(u);
        System.out.println((isNew ? "Created" : "Updated") + " user: " + email);
    }
}
