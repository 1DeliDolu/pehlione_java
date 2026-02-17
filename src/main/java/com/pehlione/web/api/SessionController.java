package com.pehlione.web.api;

import java.time.Instant;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import com.pehlione.web.auth.AuthSession;
import com.pehlione.web.auth.AuthSessionRepository;
import com.pehlione.web.auth.AuthSessionService;
import com.pehlione.web.user.User;
import com.pehlione.web.user.UserRepository;

@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

    private final UserRepository userRepo;
    private final AuthSessionRepository sessionRepo;
    private final AuthSessionService sessionService;

    public SessionController(
            UserRepository userRepo,
            AuthSessionRepository sessionRepo,
            AuthSessionService sessionService) {
        this.userRepo = userRepo;
        this.sessionRepo = sessionRepo;
        this.sessionService = sessionService;
    }

    @GetMapping
    public List<SessionDto> list(Authentication authentication) {
        User user = userRepo.findByEmail(authentication.getName()).orElseThrow();
        String currentSid = null;
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            currentSid = jwtAuth.getToken().getClaimAsString("sid");
        }

        String current = currentSid;
        return sessionService.listForUser(user.getId()).stream()
                .map(s -> SessionDto.from(s, current))
                .toList();
    }

    @PostMapping("/{sessionId}/revoke")
    public ResponseEntity<Void> revokeOne(
            Authentication authentication,
            @PathVariable("sessionId") String sessionId) {
        User user = userRepo.findByEmail(authentication.getName()).orElseThrow();
        AuthSession session = sessionRepo.findByPublicIdAndUserId(sessionId, user.getId()).orElseThrow();
        sessionService.revokeSession(session);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/revoke-all")
    public ResponseEntity<Void> revokeAll(Authentication authentication) {
        User user = userRepo.findByEmail(authentication.getName()).orElseThrow();
        sessionService.revokeAllForUser(user.getId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{sessionId}")
    @Transactional
    public ResponseEntity<Void> rename(
            Authentication authentication,
            @PathVariable("sessionId") String sessionId,
            @RequestBody RenameRequest req) {
        User user = userRepo.findByEmail(authentication.getName()).orElseThrow();
        AuthSession session = sessionRepo.findByPublicIdAndUserId(sessionId, user.getId()).orElseThrow();

        String name = req.deviceName();
        if (name != null) {
            name = name.trim();
            if (name.length() > 64) {
                name = name.substring(0, 64);
            }
            if (name.isBlank()) {
                name = null;
            }
        }

        session.setDeviceName(name);
        session.setLastSeenAt(Instant.now());
        return ResponseEntity.noContent().build();
    }

    public record SessionDto(
            String sessionId,
            String deviceName,
            Instant createdAt,
            Instant lastSeenAt,
            String ip,
            String userAgent,
            boolean revoked,
            Instant revokedAt,
            boolean current) {

        static SessionDto from(AuthSession s, String currentSid) {
            return new SessionDto(
                    s.getPublicId(),
                    s.getDeviceName(),
                    s.getCreatedAt(),
                    s.getLastSeenAt(),
                    s.getIp(),
                    s.getUserAgent(),
                    s.isRevoked(),
                    s.getRevokedAt(),
                    s.getPublicId().equals(currentSid));
        }
    }

    public record RenameRequest(String deviceName) {
    }
}
