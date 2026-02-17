package com.pehlione.web.security;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final class CounterWindow {
        private long startedAtMillis;
        private int count;
    }

    private final ConcurrentHashMap<String, CounterWindow> windows = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final long windowMillis;

    public AuthRateLimitFilter(
            @Value("${app.security.auth-rate-limit.max-requests:120}") int maxRequests,
            @Value("${app.security.auth-rate-limit.window-seconds:60}") long windowSeconds) {
        this.maxRequests = maxRequests;
        this.windowMillis = Math.max(1, windowSeconds) * 1000L;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.equals("/api/v1/auth/login")
                || path.equals("/api/v1/auth/refresh")
                || path.equals("/api/v1/auth/password/forgot")
                || path.equals("/api/v1/auth/password/reset"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String key = request.getRequestURI() + "|" + clientIp(request);
        long nowMillis = System.currentTimeMillis();

        CounterWindow window = windows.compute(key, (k, current) -> {
            if (current == null || nowMillis - current.startedAtMillis >= windowMillis) {
                CounterWindow fresh = new CounterWindow();
                fresh.startedAtMillis = nowMillis;
                fresh.count = 1;
                return fresh;
            }
            current.count++;
            return current;
        });

        if (window != null && window.count > maxRequests) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(Math.max(1, windowMillis / 1000L)));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            int commaIndex = forwardedFor.indexOf(',');
            return commaIndex >= 0 ? forwardedFor.substring(0, commaIndex).trim() : forwardedFor.trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null ? "unknown" : remoteAddr;
    }
}
