package com.pehlione.web.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.pehlione.web.mail.MailService;
import com.pehlione.web.user.User;
import com.pehlione.web.user.UserRepository;

@Service
public class PasswordResetService {

    private final UserRepository userRepo;
    private final PasswordResetTokenRepository tokenRepo;
    private final PasswordEncoder encoder;
    private final MailService mailService;
    private final AuthSessionService sessionService;
    private final String publicBaseUrl;

    public PasswordResetService(
            UserRepository userRepo,
            PasswordResetTokenRepository tokenRepo,
            PasswordEncoder encoder,
            MailService mailService,
            AuthSessionService sessionService,
            @Value("${app.public-base-url:http://localhost:8083}") String publicBaseUrl) {
        this.userRepo = userRepo;
        this.tokenRepo = tokenRepo;
        this.encoder = encoder;
        this.mailService = mailService;
        this.sessionService = sessionService;
        this.publicBaseUrl = publicBaseUrl;
    }

    @Transactional
    public void requestReset(String email) {
        if (email == null || email.isBlank()) {
            return;
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        User user = userRepo.findByEmail(normalizedEmail).orElse(null);
        if (user == null) {
            return;
        }

        if (!user.isEnabled() || user.isLocked()) {
            return;
        }

        String rawToken = TokenGenerator.newRawToken();
        String hash = TokenHash.sha256Hex(rawToken);

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(hash);
        token.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
        tokenRepo.save(token);

        String link = publicBaseUrl + "/reset-password?token=" + rawToken;
        String html = """
                <p>Password reset requested.</p>
                <p>Click to reset your password:</p>
                <p><a href="%s">Reset Password</a></p>
                <p>This link expires in 30 minutes.</p>
                """.formatted(link);

        mailService.sendHtml(user.getEmail(), "Reset your password", html);
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        String hash = TokenHash.sha256Hex(rawToken);

        PasswordResetToken token = tokenRepo.findByTokenHash(hash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token"));

        if (token.getUsedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token already used");
        }

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token expired");
        }

        User user = token.getUser();
        user.setPasswordHash(encoder.encode(newPassword));
        token.setUsedAt(Instant.now());
        sessionService.revokeAllForUser(user.getId());
    }
}
