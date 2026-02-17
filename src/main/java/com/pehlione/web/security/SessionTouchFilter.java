package com.pehlione.web.security;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.pehlione.web.auth.AuthSessionRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SessionTouchFilter extends OncePerRequestFilter {

    private final AuthSessionRepository sessionRepo;

    public SessionTouchFilter(AuthSessionRepository sessionRepo) {
        this.sessionRepo = sessionRepo;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            String sid = jwtAuth.getToken().getClaimAsString("sid");
            if (sid != null && !sid.isBlank()) {
                Instant now = Instant.now();
                Instant staleBefore = now.minus(1, ChronoUnit.MINUTES);
                sessionRepo.touchIfStale(sid, now, staleBefore);
            }
        }

        chain.doFilter(req, res);
    }
}
