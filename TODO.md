Ekrandaki import **doğru**:

```java
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
```

`HttpSecurity` zaten `spring-security-config` içinden gelir ve Spring Security dokümanında örnekler de bu sınıfla yazılır. ([Home][1])

Bu hatanın anlamı: **IDE/derleyici compile classpath’ine `spring-security-config` eklenmemiş** (yani Maven projesi doğru yüklenmemiş / resolve bozulmuş / cache takılmış).

Aşağıdaki adımları sırayla yap. (Her adım bitince diğerine geç.)

---

## Adım 1 — Maven gerçekten derleyebiliyor mu? (en kritik test)

Proje kökünde (pom.xml’in olduğu yerde) çalıştır:

```bash
./mvnw -U -DskipTests clean compile
```

Wrapper yoksa:

```bash
mvn -U -DskipTests clean compile
```

* **Eğer burada da fail oluyorsa**: Maven dependency resolve ediyor ama bir yerde kırılıyordur (çıktıyı at, net noktasını söylerim).
* **Eğer burada başarılıysa**: sorun %100 **IDE cache / proje import** tarafında.

---

## Adım 2 — `spring-security-config` gerçekten geliyor mu?

Şu komutla kontrol et:

```bash
./mvnw -DskipTests dependency:tree -Dincludes=org.springframework.security:spring-security-config
```

Çıktıda buna benzer bir satır görmelisin:

```
org.springframework.security:spring-security-config:jar:7.x.x:compile
```

`HttpSecurity` bu modülde. ([Home][2])

---

## Adım 3 — IDE tarafını düzelt (VS Code / IntelliJ)

### VS Code kullanıyorsan (ekran görüntüsü buna benziyor)

1. Command Palette → **“Java: Clean Java Language Server Workspace”**
2. VS Code restart
3. Command Palette → **“Maven: Reload Project”** (varsa)

Bu adım çoğu “cannot find symbol ama Maven compile geçiyor” durumunu çözer.

### IntelliJ ise

* Maven tool window → **Reload All Maven Projects**
* Gerekirse: **File → Invalidate Caches / Restart**

---

## Adım 4 — Dosya ve sınıfı “guide” ile birebir yapalım

Aşağıdaki dosyayı **aynı isim/path** ile oluştur:

### `src/main/java/com/pehlione/web/security/WebSecurityConfig.java`

```java
package com.pehlione.web.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/home").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .permitAll()
            )
            .logout(LogoutConfigurer::permitAll);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        UserDetails user = User.withUsername("user")
            .password(encoder.encode("password"))
            .roles("USER")
            .build();
        return new InMemoryUserDetailsManager(user);
    }
}
```

Bu yapı, Spring’in “Securing Web Application” guide’ındaki SecurityFilterChain yaklaşımıyla uyumlu. ([Home][1])

---

## Adım 5 — Boot 4 notu (sende doğru)

Senin `pom.xml`’de `spring-boot-starter-webmvc` kullanman Boot 4’te normal; `spring-boot-starter-web` artık “deprecated” görünüyor. ([mvnrepository.com][3])

---

### Burada takılırsan en hızlı teşhis

Adım 1’in çıktısında **compile başarılı mı değil mi** onu söylemen yeter:

* Başarılıysa → IDE cache kesin.
* Başarısızsa → hata logunun ilk “Caused by” kısmını yapıştır, doğrudan fixleyelim.

