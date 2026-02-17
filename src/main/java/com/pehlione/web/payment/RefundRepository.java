package com.pehlione.web.payment;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface RefundRepository extends JpaRepository<Refund, Long>, JpaSpecificationExecutor<Refund> {

	Optional<Refund> findByPublicId(String publicId);

	@EntityGraph(attributePaths = { "user", "order" })
	Page<Refund> findAll(Specification<Refund> spec, Pageable pageable);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from Refund r where r.publicId = :publicId")
	Optional<Refund> findForUpdateByPublicId(@Param("publicId") String publicId);

	Optional<Refund> findTopByOrderIdOrderByCreatedAtDesc(Long orderId);
}
