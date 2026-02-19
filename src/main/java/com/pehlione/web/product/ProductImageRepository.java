package com.pehlione.web.product;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
	List<ProductImage> findByProductIdOrderBySortOrderAscIdAsc(Long productId);

	Page<ProductImage> findByProductId(Long productId, Pageable pageable);
}
