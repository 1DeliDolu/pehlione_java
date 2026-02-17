package com.pehlione.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class AuthControllerRefreshReuseTests {

	private static final Pattern REFRESH_COOKIE_PATTERN = Pattern.compile("refresh_token=([^;]*)");

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private MockMvc mockMvc;

	@BeforeEach
	void setUpMockMvc() {
		mockMvc = MockMvcBuilders.webAppContextSetup(context)
				.apply(springSecurity())
				.build();
	}

	@Test
	void reusedRefreshTokenRevokesAllActiveRefreshTokens() throws Exception {
		resetUserState();

		MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"user@pehlione.com","password":"password"}
						"""))
				.andExpect(status().isOk())
				.andReturn();

		String loginBody = loginResult.getResponse().getContentAsString();
		String accessToken = extractField(loginBody, "accessToken");
		String sessionId = extractField(loginBody, "sessionId");
		assertThat(extractJwtClaim(accessToken, "sid")).isEqualTo(sessionId);

		String rt1 = extractRefreshToken(loginResult.getResponse().getHeader(HttpHeaders.SET_COOKIE));
		assertThat(rt1).isNotBlank();

		MvcResult rotateResult = mockMvc.perform(post("/api/v1/auth/refresh")
				.cookie(new Cookie("refresh_token", rt1)))
				.andExpect(status().isOk())
				.andReturn();

		String rt2 = extractRefreshToken(rotateResult.getResponse().getHeader(HttpHeaders.SET_COOKIE));
		assertThat(rt2).isNotBlank();
		assertThat(rt2).isNotEqualTo(rt1);

		MvcResult reuseResult = mockMvc.perform(post("/api/v1/auth/refresh")
				.cookie(new Cookie("refresh_token", rt1)))
				.andExpect(status().isUnauthorized())
				.andReturn();

		String clearedCookie = reuseResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
		assertThat(clearedCookie).contains("refresh_token=");
		assertThat(clearedCookie).contains("Max-Age=0");

		mockMvc.perform(post("/api/v1/auth/refresh")
				.cookie(new Cookie("refresh_token", rt2)))
				.andExpect(status().isUnauthorized());

		Integer activeCount = jdbcTemplate.queryForObject(
				"""
						select count(*)
						from refresh_tokens rt
						join users u on u.id = rt.user_id
						where u.email = ?
						  and rt.revoked = false
						""",
				Integer.class,
				"user@pehlione.com");
		assertThat(activeCount).isZero();

		Integer reuseEvents = jdbcTemplate.queryForObject(
				"select count(*) from auth_security_events where event_type = 'REFRESH_REUSE_DETECTED'",
				Integer.class);
		assertThat(reuseEvents).isNotNull();
		assertThat(reuseEvents).isGreaterThanOrEqualTo(1);

		Boolean locked = jdbcTemplate.queryForObject(
				"select locked from users where email = ?",
				Boolean.class,
				"user@pehlione.com");
		assertThat(locked).isTrue();

		Integer revokedSessions = jdbcTemplate.queryForObject(
				"""
						select count(*)
						from auth_sessions s
						join users u on u.id = s.user_id
						where u.email = ?
						  and s.revoked = true
						""",
				Integer.class,
				"user@pehlione.com");
		assertThat(revokedSessions).isNotNull();
		assertThat(revokedSessions).isGreaterThanOrEqualTo(1);

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"user@pehlione.com","password":"password"}
						"""))
				.andExpect(status().isUnauthorized());
	}

	private String extractField(String jsonBody, String field) {
		Pattern pattern = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]+)\"");
		Matcher matcher = pattern.matcher(jsonBody);
		assertThat(matcher.find()).isTrue();
		String value = matcher.group(1);
		assertThat(value).isNotBlank();
		return value;
	}

	private String extractJwtClaim(String jwtToken, String claim) {
		String[] parts = jwtToken.split("\\.");
		assertThat(parts.length).isGreaterThanOrEqualTo(2);
		String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
		return extractField(payload, claim);
	}

	private String extractRefreshToken(String setCookieHeader) {
		assertThat(setCookieHeader).isNotBlank();
		Matcher matcher = REFRESH_COOKIE_PATTERN.matcher(setCookieHeader);
		assertThat(matcher.find()).isTrue();
		return matcher.group(1);
	}

	private void resetUserState() {
		jdbcTemplate.update("""
				update users
				set locked = false, locked_at = null, lock_reason = null
				where email = 'user@pehlione.com'
				""");
		jdbcTemplate.update("""
				delete s from auth_sessions s
				join users u on u.id = s.user_id
				where u.email = 'user@pehlione.com'
				""");
		jdbcTemplate.update("""
				delete rt from refresh_tokens rt
				join users u on u.id = rt.user_id
				where u.email = 'user@pehlione.com'
				""");
		jdbcTemplate.update("delete from auth_security_events where event_type = 'REFRESH_REUSE_DETECTED'");
	}
}
