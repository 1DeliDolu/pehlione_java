Aşağıdaki “security bölümü”, verdiğin 3 guide’daki akışın **modern karşılığıdır**:

* **Serving Web Content** → MVC + Thymeleaf (Controller + template) ([Home][1])
* **Securing Web** → `SecurityFilterChain` + form login + in-memory user ([Home][2])
* **Spring Boot** → `@SpringBootApplication` component scan mantığı ([Home][1])

> Önemli: Eğer sende `@RestController` ile `GET "/"` dönen bir HomeController varsa, bu kurulumdaki `MvcConfig` ile **path çakışır**. Ya HomeController’ı `/api` altına taşı, ya da MVC tarafında `/` mapping’ini kaldır.

---

## 0) Gerekli bağımlılıklar (pom.xml)

Guide’ların çalışması için şunlar olmalı:

* Web (MVC)
* Thymeleaf
* Security
* Thymeleaf Security extras (hello.html’de `sec:` kullanımı için) ([Home][2])

Maven:

```xml
<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
  </dependency>

  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
  </dependency>

  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
  </dependency>

  <dependency>
    <groupId>org.thymeleaf.extras</groupId>
    <artifactId>thymeleaf-extras-springsecurity6</artifactId>
  </dependency>
</dependencies>
```

---

## 1) Klasör yapısı (security bölümü)

```text
src/main/java/com/pehlione/web/
  WebApplication.java

  config/
    MvcConfig.java

  security/
    WebSecurityConfig.java

  controller/
    GreetingController.java        (Serving Web Content guide)

src/main/resources/templates/
  home.html
  hello.html
  login.html
  greeting.html
```

---

## 2) MVC (View Controller) katmanı — `MvcConfig`

Securing-web guide’daki gibi, template’leri URL’lere bağlarız (Controller yazmadan): ([Home][2])

```java
package com.pehlione.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    registry.addViewController("/").setViewName("home");
    registry.addViewController("/home").setViewName("home");
    registry.addViewController("/hello").setViewName("hello");
    registry.addViewController("/login").setViewName("login");
  }
}
```

---

## 3) Security katmanı — `SecurityFilterChain` + InMemory user

Bu, Securing Web guide’daki modern config’in aynısı:

* `/` ve `/home` **public**
* diğer her şey **authenticated**
* custom login page: `/login`
* logout: default `/logout` ([Home][2])

```java
package com.pehlione.web.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
      .authorizeHttpRequests(req -> req
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
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService userDetailsService(PasswordEncoder encoder) {
    UserDetails user = User.withUsername("user")
      .password(encoder.encode("password"))
      .roles("USER")
      .build();
    return new InMemoryUserDetailsManager(user);
  }
}
```

### Neden senin tarayıcı otomatik `/login` açıyordu?

Spring Boot’ta Security classpath’teyse “web uygulamaları default secure edilir” ve istek türüne göre `formLogin` / `httpBasic` seçer. ([Home][3])
Bu config ile **sadece** korunan sayfalar (örn `/hello`) `/login`’e yönlenir; `/` ve `/home` public kalır.

---

## 4) Serving Web Content (Controller) — `GreetingController`

Bu controller “/greeting?name=…” mantığıyla HTML üretir. ([Home][1])

```java
package com.pehlione.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class GreetingController {

  @GetMapping("/greeting")
  public String greeting(
      @RequestParam(name = "name", required = false, defaultValue = "World") String name,
      Model model) {

    model.addAttribute("name", name);
    return "greeting";
  }
}
```

> Bu endpoint şu an security config’e göre **korunur** (çünkü `/` ve `/home` dışındakiler auth ister). Eğer `/greeting` public olsun istiyorsan `requestMatchers("/", "/home", "/greeting").permitAll()` diye ekle.

---

## 5) Thymeleaf template’leri

### `templates/home.html` (Securing Web)

([Home][2])

```html
<!DOCTYPE html>
<html xmlns:th="https://www.thymeleaf.org">
<head>
  <title>Spring Security Example</title>
</head>
<body>
  <h1>Welcome!</h1>
  <p>Click <a th:href="@{/hello}">here</a> to see a greeting.</p>
</body>
</html>
```

### `templates/hello.html` (Securing Web – username + logout)

([Home][2])

```html
<!DOCTYPE html>
<html xmlns:th="https://www.thymeleaf.org"
      xmlns:sec="https://www.thymeleaf.org/thymeleaf-extras-springsecurity6">
<head>
  <title>Hello World!</title>
</head>
<body>
  <h1 th:inline="text">Hello <span th:remove="tag" sec:authentication="name">user</span>!</h1>

  <form th:action="@{/logout}" method="post">
    <input type="submit" value="Sign Out"/>
  </form>
</body>
</html>
```

### `templates/login.html` (Securing Web – error/logout mesajları + login formu)

([Home][2])

```html
<!DOCTYPE html>
<html xmlns:th="https://www.thymeleaf.org">
<head>
  <title>Spring Security Example</title>
</head>
<body>
  <div th:if="${param.error}">Invalid username and password.</div>
  <div th:if="${param.logout}">You have been logged out.</div>

  <form th:action="@{/login}" method="post">
    <div><label> User Name : <input type="text" name="username"/> </label></div>
    <div><label> Password: <input type="password" name="password"/> </label></div>
    <div><input type="submit" value="Sign In"/></div>
  </form>
</body>
</html>
```

> CSRF notu: Spring Security’de CSRF varsayılan açık. Thymeleaf, Spring’in `RequestDataValueProcessor` entegrasyonu ile unsafe method’larda CSRF hidden field ekleme işini “boilerplate’siz” halledebilir. ([Home][4])
> Eğer bir yerde CSRF hatası görürsen, form içine manuel hidden input ekleriz (o da standart pratik).

### `templates/greeting.html` (Serving Web Content)

([Home][1])

```html
<!DOCTYPE HTML>
<html xmlns:th="http://www.thymeleaf.org">
<head>
  <title>Getting Started: Serving Web Content</title>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
</head>
<body>
  <p th:text="|Hello, ${name}!|" />
</body>
</html>
```

---

## 6) Test senaryosu (senin port 8083 ile)

`application.properties`:

```properties
server.port=8083
```

Tarayıcı:

* `http://localhost:8083/` → home (public)
* `http://localhost:8083/hello` → login’e redirect

  * user/pass: `user` / `password` ([Home][2])
* `http://localhost:8083/greeting?name=Ali` → (eğer permitAll yapmadıysan login ister)

---

İstersen bir sonraki adımda bunu **e-commerce backend’e “çift filter chain”** olarak bölelim:

* `/api/**` → stateless (401, JWT)
* `/admin/**` → formLogin (session + CSRF)

Böylece hem guide mantığını öğrenmiş olursun, hem de gerçek projeye “production pattern” taşırız.

