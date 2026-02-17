Tamam — **Product 13 = Rate Limiting + Security Hardening**. Bu adımda API’yi “production-grade” yapıyoruz:

* **Rate limiting** (IP / kullanıcı bazlı, endpoint bazlı policy)
* **Brute-force & abuse koruması** (login/register/checkout/webhook)
* **HTTP security headers + firewall + request size limitleri**
* 429 yanıtları **standart JSON** (ApiException ile)

Aşağıda “kopyala-yapıştır” ilerleyebilmen için net bir paket veriyorum.

---

# 1) Maven (pom.xml) — Bucket4j + Caffeine

**pom.xml** içine ekle:

```xml
<dependency>
  <groupId>com.bucket4j</groupId>
  <artifactId>bucket4j-core</artifactId>
  <version>8.10.1</version>
</dependency>

<dependency>
  <groupId>com.github.ben-manes.caffeine</groupId>
  <artifactId>caffeine</artifactId>
  <version>3.1.8</version>
</dependency>
```

> Bucket’ları cache’te tutacağız (expireAfterAccess ile bellek şişmesin).

---

# 2) ApiErrorCode: RATE_LIMITED ekle

**`ApiErrorCode`** enum’ına ekle:

```java
RATE_LIMITED
```

429 dönerken bunu kullanacağız.

---

# 3) application.yml — limitler + request size

**`src/main/resources/application.yml`** ekle/merge:

```yaml
app:
  ratelimit:
    enabled: true
    # cache’te bucket kaç dakika idle kalınca silinsin
    bucket-ttl-minutes: 30

    policies:
      auth_ip_per_minute: 10
      auth_ip_per_hour: 100

      api_user_per_minute: 120
      api_ip_per_minute: 300

      checkout_user_per_minute: 20
      payment_user_per_minute: 20

      webhook_ip_per_minute: 120

server:
  # payload’ları sınırlamak abuse’u azaltır
  max-http-request-header-size: 16KB

spring:
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 5MB
```

---

# 4) Rate limit core: Policy + Cache + Filter

## 4.1 Properties

**`src/main/java/com/pehlione/web/security/ratelimit/RateLimitProperties.java`**

```java
package com.pehlione.web.security.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "app.ratelimit")
public class RateLimitProperties {
    private boolean enabled = true;
    private int bucketTtlMinutes = 30;
    private Map<String, Integer> policies;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getBucketTtlMinutes() { return bucketTtlMinutes; }
    public void setBucketTtlMinutes(int bucketTtlMinutes) { this.bucketTtlMinutes = bucketTtlMinutes; }

    public Map<String, Integer> getPolicies() { return policies; }
    public void setPolicies(Map<String, Integer> policies) { this.policies = policies; }
}
```

## 4.2 Filter

**`src/main/java/com/pehlione/web/security/ratelimit/RateLimitFilter.java`**

```java
package com.pehlione.web.security.ratelimit;

import com.bucket4j.*;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.pehlione.web.api.error.ApiErrorCode;
import com.pehlione.web.api.error.ApiException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties props;
    private final Cache<String, Bucket> buckets;

    public RateLimitFilter(RateLimitProperties props) {
        this.props = props;
        this.buckets = Caffeine.newBuilder()
                .expireAfterAccess(props.getBucketTtlMinutes(), TimeUnit.MINUTES)
                .maximumSize(200_000)
                .build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !props.isEnabled();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, jakarta.servlet.ServletException {

        String path = req.getRequestURI();
        String method = req.getMethod();

        // Çok temel bypass: health/info gibi endpointler rate limit istemeyebilir
        if (path.startsWith("/actuator/health") || path.equals("/actuator/info")) {
            chain.doFilter(req, res);
            return;
        }

        Policy policy = resolvePolicy(path);
        if (policy == null) {
            chain.doFilter(req, res);
            return;
        }

        String key = buildKey(req, policy);

        Bucket bucket = buckets.get(key, k -> newBucket(policy));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // observability: kalan quota header (debug için)
            res.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(req, res);
            return;
        }

        long waitNanos = probe.getNanosToWaitForRefill();
        long retryAfterSeconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(waitNanos));

        res.setStatus(429);
        res.setContentType("application/json");
        res.setHeader("Retry-After", String.valueOf(retryAfterSeconds));

        String body = """
            {"code":"%s","message":"Too many requests","retryAfterSeconds":%d}
            """.formatted(ApiErrorCode.RATE_LIMITED.name(), retryAfterSeconds);

        res.getWriter().write(body);
    }

    private Bucket newBucket(Policy p) {
        // 1 dakikalık sabit refill (burst’ü kontrol eder)
        Bandwidth bw = Bandwidth.classic(p.limitPerMinute(), Refill.intervally(p.limitPerMinute(), Duration.ofMinutes(1)));
        return Bucket4j.builder().addLimit(bw).build();
    }

    private Policy resolvePolicy(String path) {
        Map<String, Integer> cfg = props.getPolicies();
        if (cfg == null) return null;

        // 1) auth (login/register)
        if (path.startsWith("/api/v1/auth/")) {
            return Policy.ip("auth_ip", pick(cfg, "auth_ip_per_minute", 10));
        }

        // 2) webhook
        if (path.startsWith("/api/v1/webhooks/")) {
            return Policy.ip("webhook_ip", pick(cfg, "webhook_ip_per_minute", 120));
        }

        // 3) checkout/payments (kullanıcı bazlı daha sıkı)
        if (path.startsWith("/api/v1/checkout/")) {
            return Policy.userOrIp("checkout_user", pick(cfg, "checkout_user_per_minute", 20),
                    "checkout_ip", pick(cfg, "api_ip_per_minute", 300));
        }
        if (path.startsWith("/api/v1/payments/")) {
            return Policy.userOrIp("payment_user", pick(cfg, "payment_user_per_minute", 20),
                    "payment_ip", pick(cfg, "api_ip_per_minute", 300));
        }

        // 4) genel API
        if (path.startsWith("/api/v1/")) {
            return Policy.userOrIp("api_user", pick(cfg, "api_user_per_minute", 120),
                    "api_ip", pick(cfg, "api_ip_per_minute", 300));
        }

        return null;
    }

    private int pick(Map<String, Integer> cfg, String key, int def) {
        Integer v = cfg.get(key);
        return (v == null || v <= 0) ? def : v;
    }

    private String buildKey(HttpServletRequest req, Policy policy) {
        String ip = clientIp(req);

        String subject = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            subject = jwtAuth.getToken().getSubject(); // senin projede genelde email
        }

        return policy.key(ip, subject);
    }

    private String clientIp(HttpServletRequest req) {
        // reverse proxy varsa X-Forwarded-For ile (proxy güvenli ayarlanmalı)
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    // policy model
    private record Policy(String mode, String name1, int lim1, String name2, int lim2) {
        static Policy ip(String name, int perMin) { return new Policy("ip", name, perMin, null, 0); }
        static Policy userOrIp(String userName, int userPerMin, String ipName, int ipPerMin) {
            return new Policy("userOrIp", userName, userPerMin, ipName, ipPerMin);
        }

        int limitPerMinute() { return lim1; }

        String key(String ip, String subject) {
            if ("ip".equals(mode)) return name1 + ":" + ip;
            if (subject != null && !subject.isBlank()) return name1 + ":" + subject;
            return name2 + ":" + ip;
        }
    }
}
```

### Neden böyle?

* **Auth/Webhook**: kimlik yok → IP bazlı.
* **Checkout/Payment**: abuse en riskli alan → user bazlı daha sıkı.
* **Genel API**: user bazlı, user yoksa IP bazlı.

> İstersen “burst” + “sustained” (ör. 20/min + 500/day) gibi çift limit de ekleriz (Bucket4j ile çok kolay).

---

## 4.3 ConfigurationProperties enable

**`src/main/java/com/pehlione/web/security/ratelimit/RateLimitConfig.java`**

```java
package com.pehlione.web.security.ratelimit;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig {}
```

---

# 5) SecurityConfig: Filter’ı doğru yere tak (API chain)

JWT auth sonrası user’ı görebilmek için **BearerTokenAuthenticationFilter’dan sonra** ekle.

SecurityConfig’te API filter chain’de:

```java
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;

// ...
http.addFilterAfter(rateLimitFilter, BearerTokenAuthenticationFilter.class);
```

`RateLimitFilter` bean zaten `@Component`. SecurityConfig constructor’ında inject et.

---

# 6) Security hardening: HTTP firewall + headers

## 6.1 StrictHttpFirewall

**`src/main/java/com/pehlione/web/security/FirewallConfig.java`**

```java
package com.pehlione.web.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

@Configuration
public class FirewallConfig {

    @Bean
    public HttpFirewall httpFirewall() {
        StrictHttpFirewall fw = new StrictHttpFirewall();
        fw.setAllowBackSlash(false);
        fw.setAllowUrlEncodedSlash(false);
        fw.setAllowUrlEncodedDoubleSlash(false);
        fw.setAllowUrlEncodedPercent(false);
        fw.setAllowSemicolon(false);
        return fw;
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer(HttpFirewall firewall) {
        return web -> web.httpFirewall(firewall);
    }
}
```

## 6.2 Security headers

API chain’de:

```java
http.headers(h -> h
    .contentTypeOptions(c -> {})
    .frameOptions(f -> f.sameOrigin())
    .referrerPolicy(r -> r.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
);
```

> HSTS sadece HTTPS arkasındaysan anlamlı. Reverse proxy + TLS varsa ekleriz.

---

# 7) Bonus hardening (çok işe yarar)

### 7.1 Forwarded headers (proxy arkasında doğru IP)

`application.yml`:

```yaml
server:
  forward-headers-strategy: framework
```

### 7.2 Webhook request body limit (abuse)

Zaten multipart limit var; webhook JSON için ayrıca nginx/ingress’te de limit koymak iyi.

### 7.3 CORS

Frontend ayrı domain’den gelecekse `CorsConfigurationSource` ile whitelist + method/header set ederiz.

---

# 8) Test (hızlı)

Auth endpoint’ini 10+ kez çağırınca 429 görmelisin:

```bash
for i in $(seq 1 20); do
  curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8083/api/v1/auth/login
done
```

Checkout için kullanıcı token’ı ile:

```bash
for i in $(seq 1 30); do
  curl -s -o /dev/null -w "%{http_code}\n" \
    -H "Authorization: Bearer <USER_ACCESS_TOKEN>" \
    http://localhost:8083/api/v1/checkout/drafts/<DRAFT_ID>
done
```

---

## Product 14 (sonraki)

Rate limit + hardening sonrası genelde şunlar gelir:

* **RFC7807 Problem Details** (hata formatını standardize)
* **API versioning + deprecation strategy**
* **Partial refunds + returns**

“Product 14” dersen varsayılan olarak **Problem Details (RFC7807) + exception mapping** ile devam edeyim.
