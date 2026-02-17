package com.pehlione.web.api.product;

import static com.pehlione.web.api.product.ProductDtos.CreateProductRequest;
import static com.pehlione.web.api.product.ProductDtos.ProductResponse;
import static com.pehlione.web.api.product.ProductDtos.UpdateProductRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public Page<ProductResponse> list(
            @RequestParam(name = "q", required = false) String q,
            Pageable pageable) {
        return service.listPublic(q, pageable).map(ProductResponse::from);
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return ProductResponse.from(service.getOrThrow(id));
    }

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
        return ProductResponse.from(service.create(p));
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest req) {
        Product updated = service.update(id, p -> {
            p.setName(req.name().trim());
            p.setDescription(req.description());
            p.setPrice(req.price());
            p.setCurrency(req.currency().trim());
            p.setStockQuantity(req.stockQuantity());
            p.setStatus(req.status());
        });
        return ProductResponse.from(updated);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
