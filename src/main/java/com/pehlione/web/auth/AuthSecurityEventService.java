package com.pehlione.web.auth;

import org.springframework.stereotype.Service;

import com.pehlione.web.user.User;

@Service
public class AuthSecurityEventService {

    private final AuthSecurityEventRepository repo;

    public AuthSecurityEventService(AuthSecurityEventRepository repo) {
        this.repo = repo;
    }

    public void record(User userOrNull, String type, ClientInfo client, String details) {
        AuthSecurityEvent ev = new AuthSecurityEvent();
        ev.setUser(userOrNull);
        ev.setEventType(type);
        ev.setIp(client != null ? client.ip() : null);
        ev.setUserAgent(client != null ? client.userAgent() : null);
        ev.setDetails(details);
        repo.save(ev);
    }
}
