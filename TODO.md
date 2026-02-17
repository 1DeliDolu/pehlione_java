Tamam — **Product 2 = Category (normalized) + product_categories (Many-to-Many) + public filter**.
Hedef:

* `categories` tablosu (unique slug)
* `product_categories` join table
* Product create/update request’te `categoryIds`
* `GET /api/v1/products?category=slug` ile filtre
* Admin CRUD: category oluşturma/güncelleme

---

## 1) Flyway: categories + join table

**`src/main/resources/db/migration/V14__create_categories.sql`**

```sql
CREATE TABLE categories (
  id BIGINT NOT NULL AUTO_INCREMENT,
  slug VARCHAR(64) NOT NULL,
  name VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_categories_slug (slug),
  KEY idx_categories_name (name)
);

CREATE TABLE product_categories (
  product_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  PRIMARY KEY (product_id, category_id),
  CONSTRAINT fk_pc_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
  CONSTRAINT fk_pc_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT
);

CREATE INDEX idx_pc_category ON product_categories(category_id);
```

---

## 2) Entity: Category

**`src/main/java/com/pehlione/web/category/Category.java`**

```java
package com.pehlione.web.category;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String slug;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name="created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name="updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void preUpdate() { this.updatedAt = Instant.now(); }

    public Long getId() { return id; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
```

---

## 3) Product entity: categories ilişki alanı ekle

**`src/main/java/com/pehlione/web/product/Product.java`** içine ekle:

```java
import com.pehlione.web.category.Category;
import java.util.HashSet;
import java.util.Set;
```

Alan:

```java
@ManyToMany(fetch = FetchType.LAZY)
@JoinTable(
    name = "product_categories",
    joinColumns = @JoinColumn(name = "product_id"),
    inverseJoinColumns = @JoinColumn(name = "category_id")
)
private Set<Category> categories = new HashSet<>();

public Set<Category> getCategories() { return categories; }
public void setCategories(Set<Category> categories) { this.categories = categories; }
```

---

## 4) Repository’ler

### CategoryRepository

**`src/main/java/com/pehlione/web/category/CategoryRepository.java`**

```java
package com.pehlione.web.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
```

### ProductRepository: category filter için query

**`src/main/java/com/pehlione/web/product/ProductRepository.java`** içine ekle:

```java
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
```

```java
@Query("""
    select distinct p from Product p
    join p.categories c
    where p.status = :status and c.slug = :slug
""")
Page<Product> findActiveByCategorySlug(@Param("status") ProductStatus status,
                                      @Param("slug") String slug,
                                      Pageable pageable);
```

> `distinct` join kaynaklı duplicate’leri engeller.

---

## 5) DTO güncelle: ProductResponse categories + Create/Update categoryIds

**`src/main/java/com/pehlione/web/api/product/ProductDtos.java`** değiştir:

### Request’lere `categoryIds` ekle

```java
import java.util.Set;
```

Create:

```java
public record CreateProductRequest(
        @NotBlank @Size(max = 64) String sku,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 10000) String description,
        @NotNull @DecimalMin(value = "0.00", inclusive = false) BigDecimal price,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency,
        @Min(0) int stockQuantity,
        @NotNull ProductStatus status,
        Set<@NotNull Long> categoryIds
) {}
```

Update:

```java
public record UpdateProductRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 10000) String description,
        @NotNull @DecimalMin(value = "0.00", inclusive = false) BigDecimal price,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency,
        @Min(0) int stockQuantity,
        @NotNull ProductStatus status,
        Set<@NotNull Long> categoryIds
) {}
```

### Response’a categories ekle

Category DTO:

```java
public record CategoryRef(Long id, String slug, String name) {}
```

ProductResponse:

```java
public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        String currency,
        int stockQuantity,
        ProductStatus status,
        java.util.List<CategoryRef> categories,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProductResponse from(Product p) {
        var cats = p.getCategories().stream()
                .map(c -> new CategoryRef(c.getId(), c.getSlug(), c.getName()))
                .toList();

        return new ProductResponse(
                p.getId(), p.getSku(), p.getName(), p.getDescription(),
                p.getPrice(), p.getCurrency(), p.getStockQuantity(),
                p.getStatus(), cats, p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}
```

---

## 6) ProductService: categoryIds set et

**`src/main/java/com/pehlione/web/product/ProductService.java`** constructor’a `CategoryRepository` ekle ve category resolve methodu yaz:

```java
import com.pehlione.web.category.Category;
import com.pehlione.web.category.CategoryRepository;
import java.util.HashSet;
import java.util.Set;
```

```java
private final CategoryRepository categoryRepo;

public ProductService(ProductRepository repo, CategoryRepository categoryRepo) {
    this.repo = repo;
    this.categoryRepo = categoryRepo;
}
```

Helper:

```java
private Set<Category> resolveCategories(Set<Long> ids) {
    if (ids == null || ids.isEmpty()) return Set.of();
    var found = new HashSet<>(categoryRepo.findAllById(ids));
    if (found.size() != ids.size()) {
        throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, "Some categoryIds not found");
    }
    return found;
}
```

Create/update sırasında set et (controller yerine service’te yapmak daha temiz ama hızlıca controller’da da olur). En temiz: ProductController’da req.categoryIds()’i service’e geç.

---

## 7) ProductController: list’te category slug filter + create/update set categories

**`src/main/java/com/pehlione/web/api/product/ProductController.java`**

### list endpoint

```java
@GetMapping
public Page<ProductResponse> list(
        @RequestParam(name="q", required=false) String q,
        @RequestParam(name="category", required=false) String categorySlug,
        Pageable pageable
) {
    return service.listPublic(q, categorySlug, pageable).map(ProductResponse::from);
}
```

### create/update categories

Create:

```java
p.setCategories(service.resolveCategoriesForController(req.categoryIds()));
```

Update:

```java
p.setCategories(service.resolveCategoriesForController(req.categoryIds()));
```

Burada “resolveCategories” private kaldığı için public wrapper ekleyelim:

**ProductService içine:**

```java
@Transactional(readOnly = true)
public Set<Category> resolveCategoriesForController(Set<Long> ids) {
    return resolveCategories(ids);
}
```

### ProductService listPublic overload

**`ProductService` içine:**

```java
@Transactional(readOnly = true)
public Page<Product> listPublic(String q, String categorySlug, Pageable pageable) {
    if (categorySlug != null && !categorySlug.isBlank()) {
        return repo.findActiveByCategorySlug(ProductStatus.ACTIVE, categorySlug.trim(), pageable);
    }
    if (q != null && !q.isBlank()) {
        return repo.findByStatusAndNameContainingIgnoreCase(ProductStatus.ACTIVE, q.trim(), pageable);
    }
    return repo.findByStatus(ProductStatus.ACTIVE, pageable);
}
```

> Not: q + category beraber istersen, JPQL’i genişletiriz. Şimdilik netlik için tek filtre.

---

## 8) Admin Category API (CRUD)

### DTO

**`src/main/java/com/pehlione/web/api/category/CategoryDtos.java`**

```java
package com.pehlione.web.api.category;

import com.pehlione.web.category.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class CategoryDtos {

    public record CreateCategoryRequest(
            @NotBlank @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$") @Size(max=64) String slug,
            @NotBlank @Size(max=255) String name
    ) {}

    public record UpdateCategoryRequest(
            @NotBlank @Size(max=255) String name
    ) {}

    public record CategoryResponse(
            Long id,
            String slug,
            String name,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static CategoryResponse from(Category c) {
            return new CategoryResponse(c.getId(), c.getSlug(), c.getName(), c.getCreatedAt(), c.getUpdatedAt());
        }
    }
}
```

### Service

**`src/main/java/com/pehlione/web/category/CategoryService.java`**

```java
package com.pehlione.web.category;

import com.pehlione.web.api.error.ApiErrorCode;
import com.pehlione.web.api.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final CategoryRepository repo;

    public CategoryService(CategoryRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public Category create(Category c) {
        if (repo.existsBySlug(c.getSlug())) {
            throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "Category slug already exists");
        }
        return repo.save(c);
    }

    @Transactional(readOnly = true)
    public Category getOrThrow(Long id) {
        return repo.findById(id).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Category not found"));
    }

    @Transactional
    public Category update(Long id, String name) {
        Category c = getOrThrow(id);
        c.setName(name.trim());
        return c;
    }

    @Transactional
    public void delete(Long id) {
        // RESTRICT FK yüzünden category silmek fail edebilir (ürün bağlıysa)
        repo.delete(getOrThrow(id));
    }
}
```

### Controller (admin write, public read list)

**`src/main/java/com/pehlione/web/api/category/CategoryController.java`**

```java
package com.pehlione.web.api.category;

import com.pehlione.web.category.Category;
import com.pehlione.web.category.CategoryRepository;
import com.pehlione.web.category.CategoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.pehlione.web.api.category.CategoryDtos.*;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryRepository repo;
    private final CategoryService service;

    public CategoryController(CategoryRepository repo, CategoryService service) {
        this.repo = repo;
        this.service = service;
    }

    // Public read
    @GetMapping
    public List<CategoryResponse> list() {
        return repo.findAll().stream().map(CategoryResponse::from).toList();
    }

    // Admin write
    @PostMapping
    public CategoryResponse create(@Valid @RequestBody CreateCategoryRequest req) {
        Category c = new Category();
        c.setSlug(req.slug().trim());
        c.setName(req.name().trim());
        return CategoryResponse.from(service.create(c));
    }

    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody UpdateCategoryRequest req) {
        return CategoryResponse.from(service.update(id, req.name()));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
```

---

## 9) Security: categories GET public, write admin

`SecurityConfig` API chain authorize’ya ekle:

```java
.requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/categories/**").permitAll()
.requestMatchers("/api/v1/categories/**").hasRole("ADMIN")
```

---

## 10) Test (curl)

### Admin create category

```bash
curl -i -X POST http://localhost:8083/api/v1/categories \
  -H "Authorization: Bearer <ADMIN_ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"slug":"tshirts","name":"T-Shirts"}'
```

### Admin create product with categoryIds

```bash
curl -i -X POST http://localhost:8083/api/v1/products \
  -H "Authorization: Bearer <ADMIN_ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "sku":"SKU-002",
    "name":"Basic Tee",
    "description":"100% cotton",
    "price":15.99,
    "currency":"EUR",
    "stockQuantity":50,
    "status":"ACTIVE",
    "categoryIds":[1]
  }'
```

### Public filter

```bash
curl -s "http://localhost:8083/api/v1/products?category=tshirts"
```

---

## Product 3 (sonraki)

