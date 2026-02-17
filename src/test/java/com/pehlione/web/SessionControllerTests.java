package com.pehlione.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SessionControllerTests {

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
	void listAndRevokeSingleSession() throws Exception {
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
		String refreshToken = extractRefreshToken(loginResult.getResponse().getHeader(HttpHeaders.SET_COOKIE));

		MvcResult listResult = mockMvc.perform(get("/api/v1/sessions")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andReturn();

		String sessionsBody = listResult.getResponse().getContentAsString();
		assertThat(sessionsBody).contains(sessionId);
		assertThat(sessionsBody).contains("\"current\":true");

		mockMvc.perform(patch("/api/v1/sessions/{sessionId}", sessionId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"deviceName":"Work Laptop"}
						"""))
				.andExpect(status().isNoContent());

		String renamedBody = mockMvc.perform(get("/api/v1/sessions")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(renamedBody).contains("\"deviceName\":\"Work Laptop\"");

		Instant oldSeen = Instant.now().minusSeconds(600);
		jdbcTemplate.update(
				"update auth_sessions set last_seen_at = ? where public_id = ?",
				Timestamp.from(oldSeen),
				sessionId);

		mockMvc.perform(get("/api/v1/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk());

		Timestamp touchedAt = jdbcTemplate.queryForObject(
				"select last_seen_at from auth_sessions where public_id = ?",
				Timestamp.class,
				sessionId);
		assertThat(touchedAt).isNotNull();
		assertThat(touchedAt.toInstant()).isAfterOrEqualTo(oldSeen.truncatedTo(ChronoUnit.SECONDS));

		mockMvc.perform(post("/api/v1/sessions/{sessionId}/revoke", sessionId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/auth/refresh")
				.cookie(new Cookie("refresh_token", refreshToken)))
				.andExpect(status().isUnauthorized());

		Integer activeInSession = jdbcTemplate.queryForObject(
				"""
						select count(*)
						from refresh_tokens rt
						join auth_sessions s on s.id = rt.session_id
						where s.public_id = ?
						  and rt.revoked = false
						""",
				Integer.class,
				sessionId);
		assertThat(activeInSession).isZero();
	}

	@Test
	void revokeAllSessionsRevokesAllRefreshTokens() throws Exception {
		resetUserState();

		MvcResult login1 = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"user@pehlione.com","password":"password"}
						"""))
				.andExpect(status().isOk())
				.andReturn();

		String accessToken1 = extractField(login1.getResponse().getContentAsString(), "accessToken");
		String refreshToken1 = extractRefreshToken(login1.getResponse().getHeader(HttpHeaders.SET_COOKIE));

		MvcResult login2 = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"user@pehlione.com","password":"password"}
						"""))
				.andExpect(status().isOk())
				.andReturn();

		String refreshToken2 = extractRefreshToken(login2.getResponse().getHeader(HttpHeaders.SET_COOKIE));

		mockMvc.perform(post("/api/v1/sessions/revoke-all")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
				.andExpect(status().isNoContent());

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
		assertThat(revokedSessions).isEqualTo(2);

		mockMvc.perform(post("/api/v1/auth/refresh")
				.cookie(new Cookie("refresh_token", refreshToken1)))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/v1/auth/refresh")
				.cookie(new Cookie("refresh_token", refreshToken2)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void unauthorizedSessionsListReturnsProblemDetails() throws Exception {
		mockMvc.perform(get("/api/v1/sessions"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(jsonPath("$.type").value("urn:problem:unauthorized"))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void notFoundEndpointReturnsProblemDetails() throws Exception {
		mockMvc.perform(get("/api/v1/products/99999999"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(jsonPath("$.type").value("urn:problem:not-found"))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.code").value("NOT_FOUND"))
				.andExpect(jsonPath("$.instance").value("/api/v1/products/99999999"));
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
