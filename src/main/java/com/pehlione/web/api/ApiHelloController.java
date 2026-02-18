package com.pehlione.web.api;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pehlione.web.api.error.ApiErrorCode;
import com.pehlione.web.api.error.ApiException;
import com.pehlione.web.user.UserRepository;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Tag(name = "Profile", description = "Authenticated profile endpoints")
@SecurityRequirement(name = "bearerAuth")
@RestController
public class ApiHelloController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ApiHelloController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/api/v1/me")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        var user = userRepository.findWithRolesByEmail(jwt.getSubject())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "User not found"));
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName())
                .sorted()
                .toList();
        return new MeResponse(
                user.getEmail(),
                roles,
                user.isEnabled(),
                user.isLocked(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    @PostMapping("/api/v1/me/password")
    public void changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequest req) {
        var user = userRepository.findByEmail(jwt.getSubject())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, "Current password is incorrect");
        }
        if (!req.newPassword().equals(req.confirmNewPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, "New password confirmation does not match");
        }
        if (passwordEncoder.matches(req.newPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, "New password must be different");
        }

        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
    }

    public record MeResponse(
            String email,
            List<String> roles,
            boolean enabled,
            boolean locked,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 72) String newPassword,
            @NotBlank @Size(min = 8, max = 72) String confirmNewPassword) {
    }
}
