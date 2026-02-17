package com.pehlione.web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			@Value("${app.security.mode:web}") String securityMode) throws Exception {

		if ("api".equalsIgnoreCase(securityMode)) {
			http
					.csrf(csrf -> csrf.disable())
					.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
					.authorizeHttpRequests(auth -> auth
							.requestMatchers("/", "/api/v1", "/api/v1/**").permitAll()
							.anyRequest().authenticated())
					.httpBasic(Customizer.withDefaults())
					.formLogin(form -> form.disable());
			return http.build();
		}

		http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/", "/login", "/api/v1", "/api/v1/**").permitAll()
						.anyRequest().authenticated())
				.formLogin(Customizer.withDefaults())
				.logout(Customizer.withDefaults());

		return http.build();
	}
}
