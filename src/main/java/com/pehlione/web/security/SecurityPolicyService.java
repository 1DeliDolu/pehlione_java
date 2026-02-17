package com.pehlione.web.security;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pehlione.web.auth.AuthSecurityEventService;
import com.pehlione.web.auth.AuthSessionService;
import com.pehlione.web.auth.ClientInfo;
import com.pehlione.web.user.User;
import com.pehlione.web.user.UserRepository;

@Service
public class SecurityPolicyService {

    public enum RefreshReuseAction {
        REVOKE_ONLY,
        LOCK_ACCOUNT
    }

    private final UserRepository userRepo;
    private final AuthSessionService sessionService;
    private final AuthSecurityEventService eventService;
    private final RefreshReuseAction action;

    public SecurityPolicyService(
            UserRepository userRepo,
            AuthSessionService sessionService,
            AuthSecurityEventService eventService,
            @Value("${app.security.refresh-reuse.action:LOCK_ACCOUNT}") RefreshReuseAction action) {
        this.userRepo = userRepo;
        this.sessionService = sessionService;
        this.eventService = eventService;
        this.action = action;
    }

    @Transactional
    public void onRefreshReuseDetected(String userEmail, ClientInfo client) {
        User user = userRepo.findByEmail(userEmail).orElse(null);

        if (user != null) {
            sessionService.revokeAllForUser(user.getId());
        }

        if (user != null && action == RefreshReuseAction.LOCK_ACCOUNT) {
            user.setLocked(true);
            user.setLockedAt(Instant.now());
            user.setLockReason("REFRESH_REUSE_DETECTED");
        }

        eventService.record(user, "REFRESH_REUSE_DETECTED", client,
                "Action=" + action + ", all sessions revoked");
    }
}
