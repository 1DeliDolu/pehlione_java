package com.pehlione.web.product;

import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pehlione.web.api.error.ApiErrorCode;
import com.pehlione.web.api.error.ApiException;
import com.pehlione.web.category.Category;
import com.pehlione.web.category.CategoryRepository;

@Service
public class ProductService {

    private final ProductRepository repo;
    private final CategoryRepository categoryRepo;

    public ProductService(ProductRepository repo, CategoryRepository categoryRepo) {
        this.repo = repo;
        this.categoryRepo = categoryRepo;
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
        return repo.findWithDetailsById(id)
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
        Page<Long> idsPage;
        if (q != null && !q.isBlank()) {
            idsPage = repo.findIdsByStatusAndNameContainingIgnoreCase(ProductStatus.ACTIVE, q.trim(), pageable);
        } else {
            idsPage = repo.findIdsByStatus(ProductStatus.ACTIVE, pageable);
        }
        if (idsPage.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, idsPage.getTotalElements());
        }

        Map<Long, Product> byId = repo.findWithDetailsByIdIn(idsPage.getContent()).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<Product> ordered = idsPage.getContent().stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .toList();

        return new PageImpl<>(ordered, pageable, idsPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<Product> listActiveByCategoryNoPaging(String categorySlug) {
        String normalized = categorySlug == null ? "" : categorySlug.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return List.of();
        }
        return repo.findActiveByCategorySlugFetchAll(ProductStatus.ACTIVE, normalized);
    }

    @Transactional(readOnly = true)
    public Set<Category> resolveCategoriesForController(Set<Long> ids) {
        return resolveCategories(ids);
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

    private Set<Category> resolveCategories(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        var found = Set.copyOf(categoryRepo.findAllById(ids));
        if (found.size() != ids.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, "Some categoryIds not found");
        }
        return found;
    }
}
