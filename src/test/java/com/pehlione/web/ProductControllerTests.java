package com.pehlione.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.springframework.mock.web.MockMultipartFile;
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
    void categoryWriteIsAdminOnlyAndPublicListWorks() throws Exception {
        String userAccess = loginAndGetAccessToken("user@pehlione.com", "password");
        String adminAccess = loginAndGetAccessToken("admin@pehlione.com", "admin123");

        mockMvc.perform(post("/api/v1/categories")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"slug":"tshirts","name":"T-Shirts"}
                        """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/categories")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccess)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"slug":"tshirts","name":"T-Shirts"}
                        """))
                .andExpect(status().isOk());

        String body = mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("\"slug\":\"tshirts\"");
        assertThat(body).contains("\"name\":\"T-Shirts\"");
    }

    @Test
    void publicListCanFilterByCategorySlug() throws Exception {
        jdbcTemplate.update(
                """
                        insert into categories (slug, name)
                        values (?, ?)
                        """,
                "tshirts",
                "T-Shirts");
        jdbcTemplate.update(
                """
                        insert into categories (slug, name)
                        values (?, ?)
                        """,
                "hoodies",
                "Hoodies");

        jdbcTemplate.update(
                """
                        insert into products (sku, name, description, price, currency, stock_quantity, status)
                        values (?, ?, ?, ?, ?, ?, ?)
                        """,
                "SKU-CAT-1",
                "Basic Tee",
                "cotton",
                new BigDecimal("15.99"),
                "EUR",
                30,
                "ACTIVE");
        jdbcTemplate.update(
                """
                        insert into products (sku, name, description, price, currency, stock_quantity, status)
                        values (?, ?, ?, ?, ?, ?, ?)
                        """,
                "SKU-CAT-2",
                "Cozy Hoodie",
                "warm",
                new BigDecimal("49.99"),
                "EUR",
                12,
                "ACTIVE");

        Long product1 = jdbcTemplate.queryForObject("select id from products where sku = ?", Long.class, "SKU-CAT-1");
        Long product2 = jdbcTemplate.queryForObject("select id from products where sku = ?", Long.class, "SKU-CAT-2");
        Long categoryTshirts = jdbcTemplate.queryForObject("select id from categories where slug = ?", Long.class, "tshirts");
        Long categoryHoodies = jdbcTemplate.queryForObject("select id from categories where slug = ?", Long.class, "hoodies");

        jdbcTemplate.update(
                "insert into product_categories (product_id, category_id) values (?, ?)",
                product1,
                categoryTshirts);
        jdbcTemplate.update(
                "insert into product_categories (product_id, category_id) values (?, ?)",
                product2,
                categoryHoodies);

        String filtered = mockMvc.perform(get("/api/v1/products").param("category", "tshirts"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(filtered).contains("Basic Tee");
        assertThat(filtered).doesNotContain("Cozy Hoodie");
    }

    @Test
    void adminImageEndpointsManageOrderAndPrimary() throws Exception {
        jdbcTemplate.update(
                """
                        insert into products (sku, name, description, price, currency, stock_quantity, status)
                        values (?, ?, ?, ?, ?, ?, ?)
                        """,
                "SKU-IMG-1",
                "Image Product",
                "has images",
                new BigDecimal("39.99"),
                "EUR",
                8,
                "ACTIVE");
        Long productId = jdbcTemplate.queryForObject("select id from products where sku = ?", Long.class, "SKU-IMG-1");
        assertThat(productId).isNotNull();

        String userAccess = loginAndGetAccessToken("user@pehlione.com", "password");
        String adminAccess = loginAndGetAccessToken("admin@pehlione.com", "admin123");

        mockMvc.perform(post("/api/v1/products/{id}/images", productId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccess)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"url":"https://cdn.example.com/blocked.jpg","altText":"blocked"}
                        """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/products/{id}/images", productId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccess)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"url":"https://cdn.example.com/p1.jpg","altText":"Front"}
                        """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/products/{id}/images", productId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccess)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"url":"https://cdn.example.com/p2.jpg","altText":"Back"}
                        """))
                .andExpect(status().isOk());

        Long image1 = jdbcTemplate.queryForObject(
                "select id from product_images where product_id = ? and url = ?",
                Long.class,
                productId,
                "https://cdn.example.com/p1.jpg");
        Long image2 = jdbcTemplate.queryForObject(
                "select id from product_images where product_id = ? and url = ?",
                Long.class,
                productId,
                "https://cdn.example.com/p2.jpg");
        assertThat(image1).isNotNull();
        assertThat(image2).isNotNull();

        Integer primaryCountBefore = jdbcTemplate.queryForObject(
                "select count(*) from product_images where product_id = ? and is_primary = true",
                Integer.class,
                productId);
        assertThat(primaryCountBefore).isEqualTo(1);

        mockMvc.perform(put("/api/v1/products/{id}/images/reorder", productId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccess)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "items":[
                            {"id":%d,"sortOrder":0,"primary":true},
                            {"id":%d,"sortOrder":1,"primary":false}
                          ]
                        }
                        """.formatted(image2, image1)))
                .andExpect(status().isOk());

        Integer image2Primary = jdbcTemplate.queryForObject(
                "select count(*) from product_images where id = ? and is_primary = true",
                Integer.class,
                image2);
        Integer image1Order = jdbcTemplate.queryForObject(
                "select sort_order from product_images where id = ?",
                Integer.class,
                image1);
        Integer image2Order = jdbcTemplate.queryForObject(
                "select sort_order from product_images where id = ?",
                Integer.class,
                image2);
        assertThat(image2Primary).isEqualTo(1);
        assertThat(image1Order).isEqualTo(1);
        assertThat(image2Order).isEqualTo(0);

        mockMvc.perform(delete("/api/v1/products/{id}/images/{imageId}", productId, image2)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccess))
                .andExpect(status().isOk());

        Integer remainingCount = jdbcTemplate.queryForObject(
                "select count(*) from product_images where product_id = ?",
                Integer.class,
                productId);
        Integer remainingPrimary = jdbcTemplate.queryForObject(
                "select count(*) from product_images where product_id = ? and is_primary = true",
                Integer.class,
                productId);
        assertThat(remainingCount).isEqualTo(1);
        assertThat(remainingPrimary).isEqualTo(1);

        String productBody = mockMvc.perform(get("/api/v1/products/{id}", productId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(productBody).contains("\"primaryImage\"");
        assertThat(productBody).contains("\"images\"");
        assertThat(productBody).contains("https://cdn.example.com/p1.jpg");
    }

    @Test
    void uploadImagesCreatesCategoryAndProductFoldersAndSupportsImagePagination() throws Exception {
        jdbcTemplate.update(
                "insert into categories (slug, name) values (?, ?) on duplicate key update name = values(name)",
                "ayakkabi",
                "Ayakkabi");
        Long categoryId = jdbcTemplate.queryForObject(
                "select id from categories where slug = ?",
                Long.class,
                "ayakkabi");

        jdbcTemplate.update(
                """
                        insert into products (sku, name, description, price, currency, stock_quantity, status)
                        values (?, ?, ?, ?, ?, ?, ?)
                        """,
                "SKU-SHOE-IMG-1",
                "Air Zoom Alpha",
                "shoe",
                new BigDecimal("129.99"),
                "EUR",
                4,
                "ACTIVE");
        Long productId = jdbcTemplate.queryForObject(
                "select id from products where sku = ?",
                Long.class,
                "SKU-SHOE-IMG-1");
        assertThat(productId).isNotNull();
        jdbcTemplate.update(
                "insert into product_categories (product_id, category_id) values (?, ?)",
                productId,
                categoryId);

        String adminAccess = loginAndGetAccessToken("admin@pehlione.com", "admin123");

        MockMultipartFile front = new MockMultipartFile(
                "files",
                "front.jpg",
                "image/jpeg",
                "front-image".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile side = new MockMultipartFile(
                "files",
                "side.png",
                "image/png",
                "side-image".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/products/{id}/images/upload", productId)
                .file(front)
                .file(side)
                .param("altText", "Shoe angle")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccess))
                .andExpect(status().isOk());

        String latestUrl = jdbcTemplate.queryForObject(
                "select url from product_images where product_id = ? order by id desc limit 1",
                String.class,
                productId);
        assertThat(latestUrl).startsWith("/uploads/product-images/ayakkabi/");

        String relativePath = latestUrl.substring("/uploads/product-images/".length());
        Path absolutePath = Path.of("target/test-uploads/product-images").resolve(relativePath);
        assertThat(Files.exists(absolutePath)).isTrue();

        String imagesPageBody = mockMvc.perform(get("/api/v1/products/{id}/images", productId)
                .param("page", "0")
                .param("size", "1"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(imagesPageBody).contains("\"items\"");
        assertThat(imagesPageBody).contains("\"page\"");
        assertThat(imagesPageBody).contains("\"totalPages\"");
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
