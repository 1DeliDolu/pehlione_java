package com.pehlione.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class InventoryControllerTests {

	private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile("\"accessToken\"\\s*:\\s*\"([^\"]+)\"");
	private static final Pattern RESERVATION_ID_PATTERN = Pattern.compile("\"reservationId\"\\s*:\\s*\"([^\"]+)\"");

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(context)
				.apply(springSecurity())
				.build();
		resetState();
	}

	@Test
	void reserveAndReleaseAreIdempotentAndAdjustStock() throws Exception {
		Long productId = insertProduct("SKU-INV-1", "Inventory Tee", 10);
		String userAccess = loginAndGetAccessToken("user@pehlione.com", "password");

		String reserveBody = mockMvc.perform(post("/api/v1/inventory/reserve")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"productId":%d,"quantity":3,"ttlMinutes":15}
						""".formatted(productId)))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String reservationId = extractField(reserveBody, RESERVATION_ID_PATTERN);
		Integer stockAfterReserve = jdbcTemplate.queryForObject(
				"select stock_quantity from products where id = ?",
				Integer.class,
				productId);
		assertThat(stockAfterReserve).isEqualTo(7);

		mockMvc.perform(post("/api/v1/inventory/reservations/{id}/release", reservationId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess))
				.andExpect(status().isOk());

		Integer stockAfterRelease = jdbcTemplate.queryForObject(
				"select stock_quantity from products where id = ?",
				Integer.class,
				productId);
		String reservationStatus = jdbcTemplate.queryForObject(
				"select status from inventory_reservations where public_id = ?",
				String.class,
				reservationId);
		assertThat(stockAfterRelease).isEqualTo(10);
		assertThat(reservationStatus).isEqualTo("RELEASED");

		mockMvc.perform(post("/api/v1/inventory/reservations/{id}/release", reservationId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess))
				.andExpect(status().isOk());

		Integer stockAfterSecondRelease = jdbcTemplate.queryForObject(
				"select stock_quantity from products where id = ?",
				Integer.class,
				productId);
		Integer reserveEvents = jdbcTemplate.queryForObject(
				"select count(*) from inventory_events where product_id = ? and type = 'RESERVE'",
				Integer.class,
				productId);
		Integer releaseEvents = jdbcTemplate.queryForObject(
				"select count(*) from inventory_events where product_id = ? and type = 'RELEASE'",
				Integer.class,
				productId);
		assertThat(stockAfterSecondRelease).isEqualTo(10);
		assertThat(reserveEvents).isEqualTo(1);
		assertThat(releaseEvents).isEqualTo(1);
	}

	@Test
	void consumeExpiredReservationRestoresStockAndReturnsConflict() throws Exception {
		Long productId = insertProduct("SKU-INV-2", "Inventory Hoodie", 6);
		String userAccess = loginAndGetAccessToken("user@pehlione.com", "password");

		String reserveBody = mockMvc.perform(post("/api/v1/inventory/reserve")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"productId":%d,"quantity":2,"ttlMinutes":1}
						""".formatted(productId)))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String reservationId = extractField(reserveBody, RESERVATION_ID_PATTERN);
		jdbcTemplate.update(
				"update inventory_reservations set expires_at = '2000-01-01 00:00:00' where public_id = ?",
				reservationId);

		mockMvc.perform(post("/api/v1/inventory/reservations/{id}/consume", reservationId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess))
				.andExpect(status().isConflict());

		String reservationStatus = jdbcTemplate.queryForObject(
				"select status from inventory_reservations where public_id = ?",
				String.class,
				reservationId);
		Integer stock = jdbcTemplate.queryForObject(
				"select stock_quantity from products where id = ?",
				Integer.class,
				productId);
		Integer releaseEvents = jdbcTemplate.queryForObject(
				"select count(*) from inventory_events where reservation_id = (select id from inventory_reservations where public_id = ?) and type = 'RELEASE'",
				Integer.class,
				reservationId);
		assertThat(reservationStatus).isEqualTo("EXPIRED");
		assertThat(stock).isEqualTo(6);
		assertThat(releaseEvents).isEqualTo(1);
	}

	@Test
	void adminRestockAndAdjustAreProtectedAndPersisted() throws Exception {
		Long productId = insertProduct("SKU-INV-3", "Inventory Cap", 5);
		String userAccess = loginAndGetAccessToken("user@pehlione.com", "password");
		String adminAccess = loginAndGetAccessToken("admin@pehlione.com", "admin123");

		mockMvc.perform(post("/api/v1/admin/inventory/products/{id}/restock", productId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"quantity":5,"reason":"user cannot restock"}
						"""))
				.andExpect(status().isForbidden());

		String restockBody = mockMvc.perform(post("/api/v1/admin/inventory/products/{id}/restock", productId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccess)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"quantity":5,"reason":"warehouse arrival"}
						"""))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(restockBody).contains("\"stockQuantity\":10");

		mockMvc.perform(post("/api/v1/admin/inventory/products/{id}/adjust", productId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccess)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"delta":-3,"reason":"damage"}
						"""))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/admin/inventory/products/{id}/adjust", productId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccess)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"delta":-10,"reason":"too much"}
						"""))
				.andExpect(status().isConflict());

		Integer stock = jdbcTemplate.queryForObject(
				"select stock_quantity from products where id = ?",
				Integer.class,
				productId);
		Integer restockEvents = jdbcTemplate.queryForObject(
				"select count(*) from inventory_events where product_id = ? and type = 'RESTOCK'",
				Integer.class,
				productId);
		Integer adjustEvents = jdbcTemplate.queryForObject(
				"select count(*) from inventory_events where product_id = ? and type = 'ADJUST'",
				Integer.class,
				productId);
		assertThat(stock).isEqualTo(7);
		assertThat(restockEvents).isEqualTo(1);
		assertThat(adjustEvents).isEqualTo(1);
	}

	private Long insertProduct(String sku, String name, int stockQuantity) {
		jdbcTemplate.update(
				"""
						insert into products (sku, name, description, price, currency, stock_quantity, status)
						values (?, ?, ?, ?, ?, ?, ?)
						""",
				sku,
				name,
				"inventory test",
				new BigDecimal("12.99"),
				"EUR",
				stockQuantity,
				"ACTIVE");
		return jdbcTemplate.queryForObject("select id from products where sku = ?", Long.class, sku);
	}

	private String loginAndGetAccessToken(String email, String password) throws Exception {
		String body = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"%s","password":"%s"}
						""".formatted(email, password)))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return extractField(body, ACCESS_TOKEN_PATTERN);
	}

	private String extractField(String body, Pattern pattern) {
		Matcher matcher = pattern.matcher(body);
		assertThat(matcher.find()).isTrue();
		return matcher.group(1);
	}

	private void resetState() {
		jdbcTemplate.update("delete from inventory_events");
		jdbcTemplate.update("delete from inventory_reservations");
		jdbcTemplate.update("delete from product_categories");
		jdbcTemplate.update("delete from products");
		jdbcTemplate.update("delete from categories");
		jdbcTemplate.update("""
				update users
				set locked = false, locked_at = null, lock_reason = null
				where email in ('user@pehlione.com', 'admin@pehlione.com')
				""");
	}
}
