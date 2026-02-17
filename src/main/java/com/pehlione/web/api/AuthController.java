package com.pehlione.web.api;

import static com.pehlione.web.auth.RefreshTokenExceptions.RefreshExpired;
import static com.pehlione.web.auth.RefreshTokenExceptions.RefreshNotFound;
import static com.pehlione.web.auth.RefreshTokenExceptions.RefreshReuseDetected;
import static com.pehlione.web.auth.RefreshTokenExceptions.RefreshRevoked;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pehlione.web.auth.AuthSecurityEventService;
import com.pehlione.web.auth.AuthSession;
import com.pehlione.web.auth.AuthSessionService;
import com.pehlione.web.auth.ClientInfo;
import com.pehlione.web.auth.RefreshTokenRepository;
import com.pehlione.web.auth.RefreshTokenService;
import com.pehlione.web.auth.TokenHash;
import com.pehlione.web.security.SecurityPolicyService;
import com.pehlione.web.user.User;
import com.pehlione.web.user.UserRepository;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthSessionService authSessionService;
    private final AuthSecurityEventService eventService;
    private final SecurityPolicyService securityPolicyService;
    private final UserRepository userRepository;

    private final String issuer;
    private final long accessMinutes;
    private final boolean refreshCookieSecure;
    private final String refreshCookieSameSite;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtEncoder jwtEncoder,
            RefreshTokenService refreshTokenService,
            RefreshTokenRepository refreshTokenRepository,
            AuthSessionService authSessionService,
            AuthSecurityEventService eventService,
            SecurityPolicyService securityPolicyService,
            UserRepository userRepository,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.access-minutes}") long accessMinutes,
            @Value("${app.security.refresh-cookie.secure:false}") boolean refreshCookieSecure,
            @Value("${app.security.refresh-cookie.same-site:Strict}") String refreshCookieSameSite) {
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.authSessionService = authSessionService;
        this.eventService = eventService;
        this.securityPolicyService = securityPolicyService;
        this.userRepository = userRepository;
        this.issuer = issuer;
        this.accessMinutes = accessMinutes;
        this.refreshCookieSecure = refreshCookieSecure;
        this.refreshCookieSameSite = refreshCookieSameSite;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @RequestBody LoginRequest req,
            HttpServletRequest request) {
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.email(), req.password()));
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(401).build();
        }
        ClientInfo client = ClientInfo.from(request);

        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("user_not_found_after_auth"));

        AuthSession session = authSessionService.createSession(user, client);
        String scope = auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .reduce((a, b) -> a + " " + b)
                .orElse("");
        String accessToken = issueAccessToken(user.getEmail(), scope, session.getPublicId());
        var issuedRefresh = refreshTokenService.issue(user, session);
        ResponseCookie cookie = buildRefreshCookie(issuedRefresh.rawToken(), issuedRefresh.expiresAt());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new TokenResponse(accessToken, "Bearer", accessMinutes * 60, issuedRefresh.sessionPublicId()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshRaw,
            HttpServletRequest request) {
        if (refreshRaw == null || refreshRaw.isBlank()) {
            return ResponseEntity.status(401).build();
        }

        ClientInfo client = ClientInfo.from(request);

        try {
            var next = refreshTokenService.rotateOrThrow(refreshRaw);
            authSessionService.touchById(next.sessionId(), client);

            User u = userRepository.findWithRolesByEmail(next.userEmail())
                    .orElseThrow(() -> new IllegalStateException("user_not_found"));

            String scope = u.getRoles().stream()
                    .map(r -> r.getName())
                    .reduce((a, b) -> a + " " + b)
                    .orElse("");
            String accessToken = issueAccessToken(u.getEmail(), scope, next.sessionPublicId());

            ResponseCookie cookie = buildRefreshCookie(next.rawToken(), next.expiresAt());
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(new TokenResponse(accessToken, "Bearer", accessMinutes * 60, next.sessionPublicId()));

        } catch (RefreshReuseDetected ex) {
            securityPolicyService.onRefreshReuseDetected(ex.getUserEmail(), client);
            return ResponseEntity.status(401)
                    .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                    .build();

        } catch (RefreshExpired ex) {
            eventService.record(null, "REFRESH_EXPIRED", client, "refresh expired");
            return ResponseEntity.status(401)
                    .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                    .build();

        } catch (RefreshRevoked | RefreshNotFound ex) {
            eventService.record(null, "REFRESH_INVALID", client, "refresh invalid/revoked/notfound");
            return ResponseEntity.status(401)
                    .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                    .build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshRaw,
            HttpServletRequest request) {
        ClientInfo client = ClientInfo.from(request);

        if (refreshRaw != null && !refreshRaw.isBlank()) {
            String hash = TokenHash.sha256Hex(refreshRaw);
            refreshTokenRepository.findSessionIdByTokenHash(hash)
                    .ifPresent(authSessionService::revokeSessionById);
            refreshTokenService.revokeIfPresent(refreshRaw);
        }

        eventService.record(null, "LOGOUT", client, "logout requested");
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .build();
    }

    private String issueAccessToken(String subjectEmail, String scope, String sid) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plus(accessMinutes, ChronoUnit.MINUTES))
                .subject(subjectEmail)
                .claim("scope", scope)
                .claim("sid", sid)
                .build();
        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    private ResponseCookie buildRefreshCookie(String rawToken, Instant expiresAt) {
        long maxAgeSeconds = Math.max(0, expiresAt.getEpochSecond() - Instant.now().getEpochSecond());
        return ResponseCookie.from("refresh_token", rawToken)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .path("/api/v1/auth")
                .sameSite(refreshCookieSameSite)
                .maxAge(maxAgeSeconds)
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

    public record LoginRequest(String email, String password) {
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds, String sessionId) {
    }
}
