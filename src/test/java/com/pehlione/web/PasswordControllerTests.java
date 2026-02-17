package com.pehlione.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.pehlione.web.auth.TokenHash;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PasswordControllerTests {

	private static final Pattern REFRESH_COOKIE_PATTERN = Pattern.compile("refresh_token=([^;]*)");

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private MockMvc mockMvc;

	@BeforeEach
	void setUpMockMvc() {
		mockMvc = MockMvcBuilders.webAppContextSetup(context)
				.apply(springSecurity())
				.build();
		resetUserState();
	}

	@AfterEach
	void tearDown() {
		resetUserState();
	}

	@Test
	void forgotAlwaysReturnsNoContent() throws Exception {
		mockMvc.perform(post("/api/v1/auth/password/forgot")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"user@pehlione.com"}
						"""))
				.andExpect(status().isNoContent());

		Integer knownUserTokens = jdbcTemplate.queryForObject(
				"""
						select count(*)
						from password_reset_tokens prt
						join users u on u.id = prt.user_id
						where u.email = ?
						""",
				Integer.class,
				"user@pehlione.com");
		assertThat(knownUserTokens).isEqualTo(1);

		mockMvc.perform(post("/api/v1/auth/password/forgot")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"missing@pehlione.com"}
						"""))
				.andExpect(status().isNoContent());

		Integer totalTokens = jdbcTemplate.queryForObject(
				"select count(*) from password_reset_tokens",
				Integer.class);
		assertThat(totalTokens).isEqualTo(1);
	}

	@Test
	void resetWithValidTokenChangesPasswordAndRevokesSessions() throws Exception {
		MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"user@pehlione.com","password":"password"}
						"""))
				.andExpect(status().isOk())
				.andReturn();

		String refreshToken = extractRefreshToken(loginResult.getResponse().getHeader(HttpHeaders.SET_COOKIE));
		Long userId = jdbcTemplate.queryForObject(
				"select id from users where email = ?",
				Long.class,
				"user@pehlione.com");
		assertThat(userId).isNotNull();

		String rawToken = "reset-token-" + System.nanoTime();
		String tokenHash = TokenHash.sha256Hex(rawToken);

		jdbcTemplate.update(
				"""
						insert into password_reset_tokens (user_id, token_hash, expires_at, used_at, created_at)
						values (?, ?, ?, null, ?)
						""",
				userId,
				tokenHash,
				Timestamp.from(Instant.now().plus(30, ChronoUnit.MINUTES)),
				Timestamp.from(Instant.now()));

		MvcResult resetResult = mockMvc.perform(post("/api/v1/auth/password/reset")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"token":"%s","newPassword":"NewPassw0rd!"}
						""".formatted(rawToken)))
				.andExpect(status().isNoContent())
				.andReturn();

		String clearedCookie = resetResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
		assertThat(clearedCookie).contains("refresh_token=");
		assertThat(clearedCookie).contains("Max-Age=0");

		mockMvc.perform(post("/api/v1/auth/refresh")
				.cookie(new Cookie("refresh_token", refreshToken)))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"user@pehlione.com","password":"password"}
						"""))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"user@pehlione.com","password":"NewPassw0rd!"}
						"""))
				.andExpect(status().isOk());

		Timestamp usedAt = jdbcTemplate.queryForObject(
				"select used_at from password_reset_tokens where token_hash = ?",
				Timestamp.class,
				tokenHash);
		assertThat(usedAt).isNotNull();
	}

	@Test
	void resetWithInvalidTokenReturnsBadRequest() throws Exception {
		mockMvc.perform(post("/api/v1/auth/password/reset")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"token":"invalid-token","newPassword":"NewPassw0rd!"}
						"""))
				.andExpect(status().isBadRequest());
	}

	private String extractRefreshToken(String setCookieHeader) {
		assertThat(setCookieHeader).isNotBlank();
		Matcher matcher = REFRESH_COOKIE_PATTERN.matcher(setCookieHeader);
		assertThat(matcher.find()).isTrue();
		return matcher.group(1);
	}

	private void resetUserState() {
		jdbcTemplate.update(
				"""
						update users
						set password_hash = ?,
						    locked = false,
						    locked_at = null,
						    lock_reason = null
						where email = 'user@pehlione.com'
						""",
				passwordEncoder.encode("password"));
		jdbcTemplate.update("""
				delete prt from password_reset_tokens prt
				join users u on u.id = prt.user_id
				where u.email = 'user@pehlione.com'
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
