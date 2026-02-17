package com.pehlione.web.api.product;

import static com.pehlione.web.api.product.ProductDtos.CreateProductRequest;
import static com.pehlione.web.api.product.ProductDtos.ProductResponse;
import static com.pehlione.web.api.product.ProductDtos.UpdateProductRequest;

import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pehlione.web.product.Product;
import com.pehlione.web.product.ProductService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Products", description = "Product catalogue endpoints")
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
	public Object list(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "category", required = false) String categorySlug,
            @ParameterObject Pageable pageable) {
        if (categorySlug != null && !categorySlug.isBlank()) {
            return service.listActiveByCategoryNoPaging(categorySlug).stream()
                    .map(ProductResponse::from)
                    .toList();
        }
        return service.listPublic(q, pageable).map(ProductResponse::from);
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable("id") Long id) {
        return ProductResponse.from(service.getOrThrow(id));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ProductResponse create(@Valid @RequestBody CreateProductRequest req) {
        Product p = new Product();
        p.setSku(req.sku().trim());
        p.setName(req.name().trim());
        p.setDescription(req.description());
        p.setPrice(req.price());
        p.setCurrency(req.currency().trim());
        p.setStockQuantity(req.stockQuantity());
        p.setStatus(req.status());
        p.setCategories(service.resolveCategoriesForController(req.categoryIds()));
        return ProductResponse.from(service.create(p));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable("id") Long id, @Valid @RequestBody UpdateProductRequest req) {
        Product updated = service.update(id, p -> {
            p.setName(req.name().trim());
            p.setDescription(req.description());
            p.setPrice(req.price());
            p.setCurrency(req.currency().trim());
            p.setStockQuantity(req.stockQuantity());
            p.setStatus(req.status());
            p.setCategories(service.resolveCategoriesForController(req.categoryIds()));
        });
        return ProductResponse.from(updated);
    }

    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id) {
        service.delete(id);
    }
}
