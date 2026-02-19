package com.pehlione.web.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

import com.pehlione.web.security.ratelimit.RateLimitFilter;
import com.pehlione.web.user.AppRole;

@Configuration
@EnableMethodSecurity // enables @PreAuthorize / @PostAuthorize on controllers
public class SecurityConfig {

        @Bean
        @Order(1)
        SecurityFilterChain apiSecurity(
                        HttpSecurity http,
                        RateLimitFilter rateLimitFilter,
                        ProblemAuthenticationEntryPoint problemAuthenticationEntryPoint,
                        ProblemAccessDeniedHandler problemAccessDeniedHandler,
                        SessionTouchFilter sessionTouchFilter) throws Exception {
                http
                                .securityMatcher("/api/**")
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                // ── Public auth endpoints ──────────────────────────────────────────
                                                .requestMatchers(
                                                                "/api/v1/auth/login",
                                                                "/api/v1/auth/refresh",
                                                                "/api/v1/auth/logout",
                                                                "/api/v1/auth/register",
                                                                "/api/v1/auth/verify",
                                                                "/api/v1/auth/password/**",
                                                                "/api/v1/webhooks/**")
                                                .permitAll()
                                                // ── Public catalogue (read-only) ───────────────────────────────────
                                                .requestMatchers(HttpMethod.GET, "/api/v1/products",
                                                                "/api/v1/products/**")
                                                .permitAll()
                                                .requestMatchers("/api/v1/products", "/api/v1/products/**")
                                                .hasRole(AppRole.ADMIN_BARE)
                                                .requestMatchers(HttpMethod.GET, "/api/v1/categories",
                                                                "/api/v1/categories/**")
                                                .permitAll()
                                                .requestMatchers("/api/v1/categories", "/api/v1/categories/**")
                                                .hasRole(AppRole.ADMIN_BARE)
                                                // ── Admin namespace ────────────────────────────────────────────────
                                                .requestMatchers("/api/v1/admin/**").hasRole(AppRole.ADMIN_BARE)
                                                // ── Department namespaces ──────────────────────────────────────────
                                                // ADMIN can access all departments; each role only its own namespace.
                                                .requestMatchers("/api/v1/dept/hr/**")
                                                .hasAnyRole(AppRole.ADMIN_BARE, AppRole.DEPT_HR_BARE)
                                                .requestMatchers("/api/v1/dept/it/**")
                                                .hasAnyRole(AppRole.ADMIN_BARE, AppRole.DEPT_IT_BARE)
                                                .requestMatchers("/api/v1/dept/process/**")
                                                .hasAnyRole(AppRole.ADMIN_BARE, AppRole.DEPT_PROCESS_BARE)
                                                .requestMatchers("/api/v1/dept/marketing/**")
                                                .hasAnyRole(AppRole.ADMIN_BARE, AppRole.DEPT_MARKETING_BARE)
                                                .requestMatchers("/api/v1/dept/finance/**")
                                                .hasAnyRole(AppRole.ADMIN_BARE, AppRole.DEPT_FINANCE_BARE)
                                                .requestMatchers("/api/v1/dept/support/**")
                                                .hasAnyRole(AppRole.ADMIN_BARE, AppRole.DEPT_SUPPORT_BARE)
                                                // ── Customer tier endpoints (any authenticated tier user) ──────────
                                                .requestMatchers("/api/v1/tier/**")
                                                .hasAnyRole(
                                                                AppRole.ADMIN_BARE,
                                                                AppRole.TIER_BRONZE_BARE,
                                                                AppRole.TIER_SILVER_BARE,
                                                                AppRole.TIER_GOLD_BARE,
                                                                AppRole.TIER_PLATINUM_BARE)
                                                // ── Everything else requires authentication ────────────────────────
                                                .anyRequest().authenticated())
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(problemAuthenticationEntryPoint)
                                                .accessDeniedHandler(problemAccessDeniedHandler))
                                .headers(headers -> headers
                                                .contentTypeOptions(contentType -> {
                                                })
                                                .frameOptions(frame -> frame.sameOrigin())
                                                .referrerPolicy(policy -> policy.policy(ReferrerPolicy.NO_REFERRER)))
                                .oauth2ResourceServer(oauth2 -> oauth2
                                                .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                                                jwtAuthenticationConverter())))
                                .formLogin(form -> form.disable())
                                .httpBasic(basic -> basic.disable())
                                .addFilterAfter(rateLimitFilter, BearerTokenAuthenticationFilter.class)
                                .addFilterAfter(sessionTouchFilter, RateLimitFilter.class);

                return http.build();
        }

        @Bean
        @Order(2)
        SecurityFilterChain webSecurity(HttpSecurity http) throws Exception {
                http
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**")
                                                .permitAll()
                                                .requestMatchers("/uploads/**").permitAll()
                                                .requestMatchers("/style.css", "/script.js", "/favicon.ico").permitAll()
                                                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**",
                                                                "/swagger-ui.html")
                                                .permitAll()
                                                .requestMatchers(
                                                                "/",
                                                                "/home",
                                                                "/products",
                                                                "/products/**",
                                                                "/adress",
                                                                "/address",
                                                                "/karte",
                                                                "/card",
                                                                "/password",
                                                                "/admin-product-create",
                                                                "/login",
                                                                "/register",
                                                                "/impressum",
                                                                "/datenschutz",
                                                                "/agb",
                                                                "/widerruf",
                                                                "/versand-zahlung",
                                                                "/kontakt",
                                                                "/error")
                                                .permitAll()
                                                .requestMatchers("/admin", "/admin/**").hasRole(AppRole.ADMIN_BARE)
                                                .requestMatchers("/dept/**").hasAnyRole(
                                                                AppRole.ADMIN_BARE,
                                                                AppRole.DEPT_HR_BARE,
                                                                AppRole.DEPT_IT_BARE,
                                                                AppRole.DEPT_PROCESS_BARE,
                                                                AppRole.DEPT_MARKETING_BARE,
                                                                AppRole.DEPT_FINANCE_BARE,
                                                                AppRole.DEPT_SUPPORT_BARE)
                                                .anyRequest().authenticated())
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .usernameParameter("email")
                                                .passwordParameter("password")
                                                .permitAll())
                                .logout(logout -> logout.permitAll());

                return http.build();
        }

        @Bean
        JwtAuthenticationConverter jwtAuthenticationConverter() {
                JwtGrantedAuthoritiesConverter gac = new JwtGrantedAuthoritiesConverter();
                gac.setAuthoritiesClaimName("scope");
                gac.setAuthorityPrefix("");

                JwtAuthenticationConverter jac = new JwtAuthenticationConverter();
                jac.setJwtGrantedAuthoritiesConverter(gac);
                return jac;
        }
}
