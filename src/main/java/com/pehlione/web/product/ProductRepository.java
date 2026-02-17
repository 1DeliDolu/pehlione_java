package com.pehlione.web.product;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    Page<Product> findByStatusAndNameContainingIgnoreCase(ProductStatus status, String q, Pageable pageable);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);
}
