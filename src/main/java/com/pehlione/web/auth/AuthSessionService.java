package com.pehlione.web.auth;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pehlione.web.user.User;

@Service
public class AuthSessionService {

    private final AuthSessionRepository sessionRepo;
    private final RefreshTokenRepository refreshRepo;

    public AuthSessionService(AuthSessionRepository sessionRepo, RefreshTokenRepository refreshRepo) {
        this.sessionRepo = sessionRepo;
        this.refreshRepo = refreshRepo;
    }

    @Transactional
    public AuthSession createSession(User user, ClientInfo client) {
        AuthSession s = new AuthSession();
        s.setPublicId(UUID.randomUUID().toString());
        s.setUser(user);

        if (client != null) {
            s.setIp(client.ip());
            s.setUserAgent(client.userAgent());
        }

        s.setLastSeenAt(Instant.now());
        s.setRevoked(false);
        return sessionRepo.save(s);
    }

    @Transactional
    public void touch(AuthSession session, ClientInfo client) {
        session.setLastSeenAt(Instant.now());
        if (client != null) {
            session.setIp(client.ip());
            session.setUserAgent(client.userAgent());
        }
    }

    @Transactional
    public void touchById(Long sessionId, ClientInfo client) {
        sessionRepo.findById(sessionId).ifPresent(session -> touch(session, client));
    }

    public List<AuthSession> listForUser(Long userId) {
        return sessionRepo.findByUserIdOrderByLastSeenAtDesc(userId);
    }

    @Transactional
    public void revokeSession(AuthSession session) {
        revokeSessionById(session.getId());
    }

    @Transactional
    public void revokeSessionById(Long sessionId) {
        refreshRepo.revokeAllActiveForSession(sessionId);
        AuthSession session = sessionRepo.findById(sessionId).orElse(null);
        if (session == null) {
            return;
        }
        Instant now = Instant.now();
        session.setRevoked(true);
        session.setRevokedAt(now);
        session.setLastSeenAt(now);
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        refreshRepo.revokeAllActiveForUser(userId);

        List<AuthSession> sessions = sessionRepo.findByUserIdOrderByLastSeenAtDesc(userId);
        Instant now = Instant.now();
        for (AuthSession s : sessions) {
            s.setRevoked(true);
            s.setRevokedAt(now);
            s.setLastSeenAt(now);
        }
    }
}
