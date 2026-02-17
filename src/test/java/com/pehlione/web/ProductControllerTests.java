package com.pehlione.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ProductControllerTests {

    private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile("\"accessToken\"\\s*:\\s*\"([^\"]+)\"");

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
    void publicListReturnsOnlyActiveProducts() throws Exception {
        jdbcTemplate.update(
                """
                        insert into products (sku, name, description, price, currency, stock_quantity, status)
                        values (?, ?, ?, ?, ?, ?, ?)
                        """,
                "SKU-ACTIVE-1",
                "Active Shirt",
                "cotton",
                new BigDecimal("19.99"),
                "EUR",
                10,
                "ACTIVE");
        jdbcTemplate.update(
                """
                        insert into products (sku, name, description, price, currency, stock_quantity, status)
                        values (?, ?, ?, ?, ?, ?, ?)
                        """,
                "SKU-DRAFT-1",
                "Draft Shirt",
                "draft",
                new BigDecimal("10.00"),
                "EUR",
                5,
                "DRAFT");

        String body = mockMvc.perform(get("/api/v1/products")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("Active Shirt");
        assertThat(body).doesNotContain("Draft Shirt");
    }

    @Test
    void writeOperationsAreAdminOnly() throws Exception {
        String userAccess = loginAndGetAccessToken("user@pehlione.com", "password");
        String adminAccess = loginAndGetAccessToken("admin@pehlione.com", "admin123");

        mockMvc.perform(post("/api/v1/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "sku":"SKU-USER-001",
                          "name":"User Product",
                          "description":"denied",
                          "price":19.99,
                          "currency":"EUR",
                          "stockQuantity":100,
                          "status":"ACTIVE"
                        }
                        """))
                .andExpect(status().isForbidden());

        MvcResult created = mockMvc.perform(post("/api/v1/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccess)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "sku":"SKU-ADMIN-001",
                          "name":"Admin Product",
                          "description":"allowed",
                          "price":29.99,
                          "currency":"EUR",
                          "stockQuantity":100,
                          "status":"ACTIVE"
                        }
                        """))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(created.getResponse().getContentAsString()).contains("SKU-ADMIN-001");
    }

    @Test
    void deleteArchivesProduct() throws Exception {
        jdbcTemplate.update(
                """
                        insert into products (sku, name, description, price, currency, stock_quantity, status)
                        values (?, ?, ?, ?, ?, ?, ?)
                        """,
                "SKU-ARCHIVE-1",
                "Archive Me",
                "to be archived",
                new BigDecimal("9.99"),
                "EUR",
                1,
                "ACTIVE");

        String adminAccess = loginAndGetAccessToken("admin@pehlione.com", "admin123");

        Long productId = jdbcTemplate.queryForObject(
                "select id from products where sku = ?",
                Long.class,
                "SKU-ARCHIVE-1");
        assertThat(productId).isNotNull();

        mockMvc.perform(delete("/api/v1/products/{id}", productId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccess))
                .andExpect(status().isOk());

        String listBody = mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(listBody).doesNotContain("Archive Me");
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

        Matcher matcher = ACCESS_TOKEN_PATTERN.matcher(body);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private void resetState() {
        jdbcTemplate.update("delete from products");
        jdbcTemplate.update("""
                update users
                set locked = false, locked_at = null, lock_reason = null
                where email in ('user@pehlione.com', 'admin@pehlione.com')
                """);
    }
}
