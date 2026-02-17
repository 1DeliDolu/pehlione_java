
---

## A) Web tarzı (Form login dursun, ama `/` public olsun)

Tarayıcıda login sayfası gelsin; ama `/` gibi bazı yollar public kalsın:

```java
package com.pehlione.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
      .authorizeHttpRequests(auth -> auth
        .requestMatchers("/", "/login", "/api/v1/**").permitAll()
        .anyRequest().authenticated()
      )
      .formLogin(Customizer.withDefaults())   // /login redirect devam eder (beklenen)
      .logout(Customizer.withDefaults());

    return http.build();
  }
}
```

> Not: Form login + session kullanıyorsan **CSRF’yi kapatma** (varsayılan açık kalsın) genelde daha doğru. CSRF neden/nerede lazım konusunu Spring dokümanı iyi özetler. ([Home][2])

---

## B) API tarzı (Redirect istemiyorum, security olsun: 401 dönsün)

Tarayıcı redirect yerine **401 Unauthorized** görmek ve API’yı “stateless” tutmak istiyorsan:

```java
package com.pehlione.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
      .csrf(csrf -> csrf.disable()) // stateless API için pratik
      .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> auth
        .requestMatchers("/", "/api/v1/**").permitAll()
        .anyRequest().authenticated()
      )
      .httpBasic(Customizer.withDefaults())   // şimdilik Basic Auth
      .formLogin(form -> form.disable());     // /login redirect KAPANIR

    return http.build();
  }
}
```

Bu yaklaşım Spring Security’nin modern yetkilendirme DSL’iyle uyumlu (`authorizeHttpRequests`). ([Home][3])

---

## Neden tarayıcıda özellikle `/login` görüyorsun?

Boot, istek “tarayıcı isteği” gibi görünüyorsa (HTML bekliyor gibi), `formLogin` seçip redirect eder. ([Home][1])

---

### Hızlı test

* Redirect olmadan API gibi test:

```bash
curl -i http://localhost:8083/
```

---


