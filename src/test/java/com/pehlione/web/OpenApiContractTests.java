package com.pehlione.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class OpenApiContractTests {

	private static final Path PUBLIC_SPEC = Path.of("api-contract", "openapi-public.json");
	private static final Path ADMIN_SPEC = Path.of("api-contract", "openapi-admin.json");
	private static final ObjectMapper OM = new ObjectMapper()
			.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

	@Autowired
	private WebApplicationContext context;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(context)
				.apply(springSecurity())
				.build();
	}

	@Test
	void publicSpecMatchesSnapshot() throws Exception {
		assertSpec("/v3/api-docs/public", PUBLIC_SPEC);
	}

	@Test
	void adminSpecMatchesSnapshot() throws Exception {
		assertSpec("/v3/api-docs/admin", ADMIN_SPEC);
	}

	private void assertSpec(String endpoint, Path snapshotPath) throws Exception {
		String actual = mockMvc.perform(get(endpoint))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String canonicalActual = canonical(actual);
		if (updateModeEnabled()) {
			Files.createDirectories(snapshotPath.getParent());
			Files.writeString(snapshotPath, canonicalActual + System.lineSeparator());
			return;
		}

		if (!Files.exists(snapshotPath)) {
			fail("OpenAPI snapshot missing: " + snapshotPath
					+ ". Run: ./mvnw -Dopenapi.snapshot.update=true -Dtest=OpenApiContractTests test");
		}

		String expected = Files.readString(snapshotPath);
		assertThat(canonicalActual).isEqualTo(canonical(expected));
	}

	private boolean updateModeEnabled() {
		return Boolean.parseBoolean(System.getProperty("openapi.snapshot.update", "false"));
	}

	private String canonical(String json) throws IOException {
		Map<String, Object> parsed = OM.readValue(json, new TypeReference<>() {
		});
		parsed.remove("servers");
		return OM.writeValueAsString(parsed);
	}
}
