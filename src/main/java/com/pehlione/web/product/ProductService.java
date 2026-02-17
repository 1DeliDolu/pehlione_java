package com.pehlione.web.product;

import java.util.Locale;
import java.util.function.Consumer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pehlione.web.api.error.ApiErrorCode;
import com.pehlione.web.api.error.ApiException;

@Service
public class ProductService {

    private final ProductRepository repo;

    public ProductService(ProductRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public Product create(Product product) {
        String normalizedSku = normalizeSku(product.getSku());
        if (repo.findBySku(normalizedSku).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "SKU already exists");
        }
        product.setSku(normalizedSku);
        product.setCurrency(normalizeCurrency(product.getCurrency()));
        return repo.save(product);
    }

    @Transactional(readOnly = true)
    public Product getOrThrow(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Product not found"));
    }

    @Transactional
    public Product update(Long id, Consumer<Product> updater) {
        Product existing = getOrThrow(id);
        updater.accept(existing);
        existing.setCurrency(normalizeCurrency(existing.getCurrency()));
        return existing;
    }

    @Transactional(readOnly = true)
    public Page<Product> listPublic(String q, Pageable pageable) {
        if (q != null && !q.isBlank()) {
            return repo.findByStatusAndNameContainingIgnoreCase(ProductStatus.ACTIVE, q.trim(), pageable);
        }
        return repo.findByStatus(ProductStatus.ACTIVE, pageable);
    }

    @Transactional
    public void delete(Long id) {
        Product existing = getOrThrow(id);
        existing.setStatus(ProductStatus.ARCHIVED);
    }

    private String normalizeSku(String sku) {
        return sku == null ? null : sku.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCurrency(String currency) {
        return currency == null ? null : currency.trim().toUpperCase(Locale.ROOT);
    }
}
