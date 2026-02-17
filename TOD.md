
---

## 1) Security: password reset endpoint’leri public olsun

**`src/main/java/com/pehlione/web/security/SecurityConfig.java`** (API chain authorize kısmı)

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers(
        "/api/v1/auth/login",
        "/api/v1/auth/refresh",
        "/api/v1/auth/logout",
        "/api/v1/auth/register",
        "/api/v1/auth/verify",
        "/api/v1/auth/password/**"   // ✅ forgot + reset
    ).permitAll()
    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
    .anyRequest().authenticated()
)
```

---

## 2) Rate limit: password reset’i de kapsa

**`src/main/java/com/pehlione/web/security/AuthRateLimitFilter.java`** içindeki `shouldNotFilter`’ı genişlet:

```java
@Override
protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return !(path.equals("/api/v1/auth/login")
            || path.equals("/api/v1/auth/refresh")
            || path.equals("/api/v1/auth/password/forgot")
            || path.equals("/api/v1/auth/password/reset"));
}
```

---

## 3) DB: Password reset token tablosu

Eğer daha önce eklemediysen:

**`src/main/resources/db/migration/V11__create_password_reset_tokens.sql`**

```sql
CREATE TABLE password_reset_tokens (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  token_hash CHAR(64) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  used_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_prt_hash (token_hash),
  KEY idx_prt_user (user_id),
  CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

---

## 4) Entity + Repository

### Entity

**`src/main/java/com/pehlione/web/auth/PasswordResetToken.java`**

```java
package com.pehlione.web.auth;

import com.pehlione.web.user.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name="password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="user_id", nullable = false)
    private User user;

    @Column(name="token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name="expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name="used_at")
    private Instant usedAt;

    @Column(name="created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getUsedAt() { return usedAt; }
    public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }

    public Instant getCreatedAt() { return createdAt; }
}
```

### Repository

**`src/main/java/com/pehlione/web/auth/PasswordResetTokenRepository.java`**

```java
package com.pehlione.web.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
}
```

---

## 5) TokenGenerator (yoksa ekle)

**`src/main/java/com/pehlione/web/auth/TokenGenerator.java`**

```java
package com.pehlione.web.auth;

import java.security.SecureRandom;
import java.util.Base64;

public final class TokenGenerator {
    private static final SecureRandom random = new SecureRandom();
    private TokenGenerator() {}

    public static String newRawToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
```

`TokenHash.sha256Hex(...)` zaten sende var.

---

## 6) PasswordResetService (token üret + mail + reset + revoke-all sessions)

**`src/main/java/com/pehlione/web/auth/PasswordResetService.java`**

```java
package com.pehlione.web.auth;

import com.pehlione.web.api.error.ApiErrorCode;
import com.pehlione.web.api.error.ApiException;
import com.pehlione.web.mail.MailService;
import com.pehlione.web.user.User;
import com.pehlione.web.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

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
            @Value("${app.public-base-url}") String publicBaseUrl
    ) {
        this.userRepo = userRepo;
        this.tokenRepo = tokenRepo;
        this.encoder = encoder;
        this.mailService = mailService;
        this.sessionService = sessionService;
        this.publicBaseUrl = publicBaseUrl;
    }

    /**
     * Email enumeration yapmamak için controller her zaman 204 dönecek.
     * Burada user varsa mail atacağız; yoksa sessizce bitecek.
     */
    @Transactional
    public void requestReset(String email) {
        User u = userRepo.findByEmail(email).orElse(null);
        if (u == null) return;

        // Policy: istersen sadece verified+enabled için mail at
        if (!u.isEnabled() || u.isLocked() || !u.isEmailVerified()) return;

        String raw = TokenGenerator.newRawToken();
        String hash = TokenHash.sha256Hex(raw);

        PasswordResetToken t = new PasswordResetToken();
        t.setUser(u);
        t.setTokenHash(hash);
        t.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES)); // 30 dk
        tokenRepo.save(t);

        // Link genelde frontend sayfasına gider; token query string ile taşınır.
        String link = publicBaseUrl + "/reset-password?token=" + raw;

        String html = """
            <p>Password reset requested.</p>
            <p>Click to reset your password:</p>
            <p><a href="%s">Reset Password</a></p>
            <p>This link expires in 30 minutes.</p>
            """.formatted(link);

        mailService.sendHtml(u.getEmail(), "Reset your password", html);
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        String hash = TokenHash.sha256Hex(rawToken);

        PasswordResetToken t = tokenRepo.findByTokenHash(hash)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.CONFLICT, "Invalid token"));

        if (t.getUsedAt() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.CONFLICT, "Token already used");
        }
        if (t.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.CONFLICT, "Token expired");
        }

        User u = t.getUser();

        // Şifreyi değiştir
        u.setPasswordHash(encoder.encode(newPassword));

        // Token'ı one-time yap
        t.setUsedAt(Instant.now());

        // Güvenlik: tüm cihazlardan logout (refresh+session kill switch)
        sessionService.revokeAllForUser(u.getId());
    }
}
```

---

## 7) Controller: forgot + reset (her zaman güvenli davranış)

**`src/main/java/com/pehlione/web/api/PasswordController.java`**

```java
package com.pehlione.web.api;

import com.pehlione.web.auth.PasswordResetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/password")
public class PasswordController {

    private final PasswordResetService resetService;

    // refresh cookie temizlemek için (senin AuthController ile aynı path/samesite ayarına uygun)
    // Burada path'i "/api/v1/auth" tutuyoruz ki cookie kesin temizlensin.
    public PasswordController(PasswordResetService resetService) {
        this.resetService = resetService;
    }

    @PostMapping("/forgot")
    public ResponseEntity<Void> forgot(@Valid @RequestBody ForgotRequest req) {
        // Always 204: email enumeration yok
        resetService.requestReset(req.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> reset(@Valid @RequestBody ResetRequest req) {
        resetService.resetPassword(req.token(), req.newPassword());

        // password reset sonrası refresh cookie temizle (client tarafı güvenli)
        ResponseCookie cleared = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(false) // dev; prod’da config’ten alabilirsin
                .path("/api/v1/auth")
                .sameSite("Strict")
                .maxAge(0)
                .build();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cleared.toString())
                .build();
    }

    public record ForgotRequest(@Email @NotBlank String email) {}

    public record ResetRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8, max = 72) String newPassword
    ) {}
}
```

> Cookie temizlemede `secure=false` dev içindi. İstersen bunu `@Value("${app.security.refresh-cookie.secure}")` ile konfigürasyondan okuyacak hale getiririm (AuthController’daki gibi).

---

## 8) Önceki adım: VerificationService placeholder düzeltmesi

Senin kodun derlenebilir olsun diye **VerificationService**’te token üretimini `TokenGenerator` ile yap.

**`src/main/java/com/pehlione/web/auth/VerificationService.java`** içindeki token üretimini şu şekilde düzelt:

```java
String rawToken = TokenGenerator.newRawToken();
String hash = TokenHash.sha256Hex(rawToken);
```

(placeholder olan satırları tamamen kaldır.)

---

## 9) Test (MailHog ile en kolay)

### (A) MailHog önerisi

Localde mail test için en pratik: MailHog (SMTP 1025, UI 8025).

`application.properties`:

```properties
spring.mail.host=localhost
spring.mail.port=1025
app.public-base-url=http://localhost:8083
```

### (B) Forgot

```bash
curl -i -X POST http://localhost:8083/api/v1/auth/password/forgot \
  -H "Content-Type: application/json" \
  -d '{"email":"user@pehlione.com"}'
```

Her zaman `204` dönmeli.

MailHog UI’da gelen mailde linkte `token=...` olacak.

### (C) Reset

```bash
curl -i -X POST http://localhost:8083/api/v1/auth/password/reset \
  -H "Content-Type: application/json" \
  -d '{"token":"<MAILDEKI_TOKEN>","newPassword":"NewPassw0rd!"}'
```

Sonra login’i yeni şifreyle dene.

---

### Bu adımın güvenlik sonuçları

* Reset başarılı olunca **tüm cihazlardaki refresh/session revoke** edildi → kullanıcı yeniden login olmak zorunda.
* Forgot endpoint’i **email enumeration** yapmaz (her zaman 204).

---

