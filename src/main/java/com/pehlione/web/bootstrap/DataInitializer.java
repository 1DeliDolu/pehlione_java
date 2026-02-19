package com.pehlione.web.bootstrap;

import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.pehlione.web.user.AppRole;
import com.pehlione.web.user.CustomerTier;
import com.pehlione.web.user.Department;
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

            // ── Core roles ────────────────────────────────────────────────────
            Role roleUser = ensureRole(roleRepository, AppRole.USER);
            Role roleAdmin = ensureRole(roleRepository, AppRole.ADMIN);

            // ── Department roles ──────────────────────────────────────────────
            Role roleHr = ensureRole(roleRepository, AppRole.DEPT_HR);
            Role roleIt = ensureRole(roleRepository, AppRole.DEPT_IT);
            Role roleProcess = ensureRole(roleRepository, AppRole.DEPT_PROCESS);
            Role roleMarketing = ensureRole(roleRepository, AppRole.DEPT_MARKETING);
            Role roleFinance = ensureRole(roleRepository, AppRole.DEPT_FINANCE);
            Role roleSupport = ensureRole(roleRepository, AppRole.DEPT_SUPPORT);

            // ── Customer tier roles ───────────────────────────────────────────
            Role tierBronze = ensureRole(roleRepository, AppRole.TIER_BRONZE);
            Role tierSilver = ensureRole(roleRepository, AppRole.TIER_SILVER);
            Role tierGold = ensureRole(roleRepository, AppRole.TIER_GOLD);
            Role tierPlatinum = ensureRole(roleRepository, AppRole.TIER_PLATINUM);

            // ── Seed users ────────────────────────────────────────────────────

            // Core
            ensureUser(userRepository, passwordEncoder,
                    "user@pehlione.com", "password", Set.of(roleUser),
                    null, CustomerTier.BRONZE);
            ensureUser(userRepository, passwordEncoder,
                    "admin@pehlione.com", "admin123", Set.of(roleUser, roleAdmin),
                    null, null);

            // Department members (each also has ROLE_USER for storefront access)
            ensureDeptUser(userRepository, passwordEncoder, roleRepository,
                    "hr@pehlione.com", "hr123", roleHr, Department.HR);
            ensureDeptUser(userRepository, passwordEncoder, roleRepository,
                    "it@pehlione.com", "it123", roleIt, Department.IT);
            ensureDeptUser(userRepository, passwordEncoder, roleRepository,
                    "process@pehlione.com", "process123", roleProcess, Department.PROCESS);
            ensureDeptUser(userRepository, passwordEncoder, roleRepository,
                    "marketing@pehlione.com", "marketing123", roleMarketing, Department.MARKETING);
            ensureDeptUser(userRepository, passwordEncoder, roleRepository,
                    "finance@pehlione.com", "finance123", roleFinance, Department.FINANCE);
            ensureDeptUser(userRepository, passwordEncoder, roleRepository,
                    "support@pehlione.com", "support123", roleSupport, Department.SUPPORT);

            // Customer tier demos
            ensureUser(userRepository, passwordEncoder,
                    "bronze@pehlione.com", "bronze123", Set.of(roleUser, tierBronze),
                    null, CustomerTier.BRONZE);
            ensureUser(userRepository, passwordEncoder,
                    "silver@pehlione.com", "silver123", Set.of(roleUser, tierSilver),
                    null, CustomerTier.SILVER);
            ensureUser(userRepository, passwordEncoder,
                    "gold@pehlione.com", "gold123", Set.of(roleUser, tierGold),
                    null, CustomerTier.GOLD);
            ensureUser(userRepository, passwordEncoder,
                    "platinum@pehlione.com", "platinum123", Set.of(roleUser, tierPlatinum),
                    null, CustomerTier.PLATINUM);
        };
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Role ensureRole(RoleRepository repo, String roleName) {
        return repo.findByName(roleName).orElseGet(() -> {
            Role r = new Role();
            r.setName(roleName);
            return repo.save(r);
        });
    }

    private void ensureDeptUser(
            UserRepository userRepository,
            PasswordEncoder encoder,
            RoleRepository roleRepository,
            String email,
            String rawPassword,
            Role deptRole,
            Department department) {
        Role roleUser = ensureRole(roleRepository, AppRole.USER);
        ensureUser(userRepository, encoder, email, rawPassword,
                Set.of(roleUser, deptRole), department.name(), null);
    }

    private void ensureUser(
            UserRepository userRepository,
            PasswordEncoder encoder,
            String email,
            String rawPassword,
            Set<Role> desiredRoles,
            String department,
            CustomerTier tier) {
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

        if (department != null && u.getDepartment() == null) {
            u.setDepartment(department);
        }
        if (tier != null && u.getCustomerTier() == null) {
            u.setCustomerTier(tier);
        }

        userRepository.save(u);
        System.out.println((isNew ? "Created" : "Updated") + " user: " + email);
    }
}
