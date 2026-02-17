package com.pehlione.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
		"app.ratelimit.enabled=true",
		"app.ratelimit.policies.auth_ip_per_minute=2",
		"app.ratelimit.policies.auth_ip_per_hour=2"
})
class RateLimitFilterTests {

	@Autowired
	private WebApplicationContext context;

	private MockMvc mockMvc;

	@BeforeEach
	void setUpMockMvc() {
		mockMvc = MockMvcBuilders.webAppContextSetup(context)
				.apply(springSecurity())
				.build();
	}

	@Test
	void authEndpointReturns429WithRetryAfterAndBodyFieldsWhenLimitExceeded() throws Exception {
		mockMvc.perform(post("/api/v1/auth/password/forgot")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"rate-limit@pehlione.com"}
						"""))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/auth/password/forgot")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"rate-limit@pehlione.com"}
						"""))
				.andExpect(status().isNoContent());

		MvcResult limited = mockMvc.perform(post("/api/v1/auth/password/forgot")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"rate-limit@pehlione.com"}
						"""))
				.andExpect(status().isTooManyRequests())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(header().exists("Retry-After"))
				.andExpect(jsonPath("$.type").value("urn:problem:rate-limited"))
				.andExpect(jsonPath("$.title").value("Too Many Requests"))
				.andExpect(jsonPath("$.status").value(429))
				.andExpect(jsonPath("$.code").value("RATE_LIMITED"))
				.andExpect(jsonPath("$.retryAfterSeconds").isNumber())
				.andReturn();

		String retryAfter = limited.getResponse().getHeader("Retry-After");
		assertThat(retryAfter).isNotBlank();
		assertThat(Long.parseLong(retryAfter)).isPositive();
	}
}
