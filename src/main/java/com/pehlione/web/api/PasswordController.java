package com.pehlione.web.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pehlione.web.auth.PasswordResetService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/auth/password")
public class PasswordController {

    private final PasswordResetService resetService;
    private final boolean refreshCookieSecure;
    private final String refreshCookieSameSite;

    public PasswordController(
            PasswordResetService resetService,
            @Value("${app.security.refresh-cookie.secure:false}") boolean refreshCookieSecure,
            @Value("${app.security.refresh-cookie.same-site:Strict}") String refreshCookieSameSite) {
        this.resetService = resetService;
        this.refreshCookieSecure = refreshCookieSecure;
        this.refreshCookieSameSite = refreshCookieSameSite;
    }

    @PostMapping("/forgot")
    public ResponseEntity<Void> forgot(@Valid @RequestBody ForgotRequest req) {
        resetService.requestReset(req.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> reset(@Valid @RequestBody ResetRequest req) {
        resetService.resetPassword(req.token(), req.newPassword());
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .build();
    }

    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .path("/api/v1/auth")
                .sameSite(refreshCookieSameSite)
                .maxAge(0)
                .build();
    }

    public record ForgotRequest(@Email @NotBlank String email) {
    }

    public record ResetRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8, max = 72) String newPassword) {
    }
}
