package com.pehlione.web.auth;

import static com.pehlione.web.auth.RefreshTokenExceptions.RefreshExpired;
import static com.pehlione.web.auth.RefreshTokenExceptions.RefreshNotFound;
import static com.pehlione.web.auth.RefreshTokenExceptions.RefreshReuseDetected;
import static com.pehlione.web.auth.RefreshTokenExceptions.RefreshRevoked;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pehlione.web.user.User;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repo;

    private final long refreshDays;

    public RefreshTokenService(
            RefreshTokenRepository repo,
            @Value("${app.jwt.refresh-days:7}") long refreshDays) {
        this.repo = repo;
        this.refreshDays = refreshDays;
    }

    public record IssuedRefreshToken(
            String rawToken,
            String tokenHash,
            Instant expiresAt,
            String userEmail,
            String sessionPublicId,
            Long sessionId) {
    }

    public IssuedRefreshToken issue(User user, AuthSession session) {
        String raw = TokenGenerator.newRawToken();
        String hash = TokenHash.sha256Hex(raw);
        Instant exp = Instant.now().plus(refreshDays, ChronoUnit.DAYS);

        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setSession(session);
        rt.setTokenHash(hash);
        rt.setExpiresAt(exp);
        rt.setRevoked(false);

        repo.save(rt);
        return new IssuedRefreshToken(raw, hash, exp, user.getEmail(), session.getPublicId(), session.getId());
    }

    @Transactional(noRollbackFor = RefreshReuseDetected.class)
    public IssuedRefreshToken rotateOrThrow(String presentedRawToken) {
        String presentedHash = TokenHash.sha256Hex(presentedRawToken);

        RefreshToken current = repo.findByTokenHash(presentedHash)
                .orElseThrow(RefreshNotFound::new);

        // Reuse detection: old rotated token was presented again.
        if (current.getReplacedByHash() != null) {
            repo.revokeAllActiveForUser(current.getUser().getId());
            throw new RefreshReuseDetected(current.getUser().getId(), current.getUser().getEmail());
        }
        if (current.isRevoked()) {
            throw new RefreshRevoked();
        }
        if (current.getExpiresAt().isBefore(Instant.now())) {
            throw new RefreshExpired();
        }
        if (current.getSession() == null) {
            repo.revokeAllActiveForUser(current.getUser().getId());
            throw new RefreshRevoked();
        }

        IssuedRefreshToken next = issue(current.getUser(), current.getSession());

        current.setRevoked(true);
        current.setReplacedByHash(next.tokenHash());
        current.setLastUsedAt(Instant.now());

        return next;
    }

    @Transactional
    public void revokeIfPresent(String presentedRawToken) {
        String hash = TokenHash.sha256Hex(presentedRawToken);
        repo.findByTokenHash(hash).ifPresent(rt -> {
            rt.setRevoked(true);
            rt.setLastUsedAt(Instant.now());
        });
    }

    @Transactional
    public void revokeAllForPresentedToken(String presentedRawToken) {
        String hash = TokenHash.sha256Hex(presentedRawToken);
        repo.findByTokenHash(hash).ifPresent(rt -> {
            repo.revokeAllActiveForUser(rt.getUser().getId());
            rt.setRevoked(true);
            rt.setLastUsedAt(Instant.now());
        });
    }

}
