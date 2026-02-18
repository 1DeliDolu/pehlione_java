package com.pehlione.web.controller;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.pehlione.web.user.Role;
import com.pehlione.web.user.RoleRepository;
import com.pehlione.web.user.User;
import com.pehlione.web.user.UserRepository;

@Controller
public class RegistrationController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationController(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public String register(
            @RequestParam(name = "email", defaultValue = "") String rawEmail,
            @RequestParam(name = "password", defaultValue = "") String password,
            @RequestParam(name = "confirmPassword", defaultValue = "") String confirmPassword,
            RedirectAttributes redirectAttributes) {
        String email = normalizeEmail(rawEmail);

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return fail(redirectAttributes, "Please enter a valid email address.");
        }
        if (password.length() < 8 || password.length() > 72) {
            return fail(redirectAttributes, "Password length must be between 8 and 72 characters.");
        }
        if (!password.equals(confirmPassword)) {
            return fail(redirectAttributes, "Password confirmation does not match.");
        }
        if (userRepository.existsByEmail(email)) {
            return fail(redirectAttributes, "An account with this email already exists.");
        }

        Role roleUser = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("ROLE_USER");
                    return roleRepository.save(role);
                });

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setEnabled(true);
        user.setRoles(new HashSet<>(Set.of(roleUser)));

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            return fail(redirectAttributes, "An account with this email already exists.");
        }

        redirectAttributes.addFlashAttribute(
                "registerSuccess",
                "Account created successfully. Please sign in.");
        return "redirect:/login";
    }

    private static String normalizeEmail(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String fail(RedirectAttributes redirectAttributes, String message) {
        redirectAttributes.addFlashAttribute("registerError", message);
        return "redirect:/register";
    }
}
