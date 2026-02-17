package com.pehlione.web.order;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

	Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

	@EntityGraph(attributePaths = { "items", "items.product", "shippingAddress" })
	Optional<Order> findByPublicIdAndUserId(String publicId, Long userId);

	@EntityGraph(attributePaths = { "user" })
	Page<Order> findAll(Specification<Order> spec, Pageable pageable);

	@EntityGraph(attributePaths = { "user", "items", "items.product", "shippingAddress" })
	Optional<Order> findByPublicId(String publicId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select o from Order o where o.id = :id")
	Optional<Order> findForUpdateWithItems(@Param("id") Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select o from Order o where o.publicId = :publicId")
	Optional<Order> findForUpdateByPublicId(@Param("publicId") String publicId);
}
