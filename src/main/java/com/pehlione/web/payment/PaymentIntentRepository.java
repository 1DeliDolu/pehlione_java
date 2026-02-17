package com.pehlione.web.payment;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface PaymentIntentRepository extends JpaRepository<PaymentIntent, Long> {

	Optional<PaymentIntent> findByPublicId(String publicId);

	Optional<PaymentIntent> findByIdempotencyKey(String idempotencyKey);

	Optional<PaymentIntent> findTopByOrderIdOrderByIdDesc(Long orderId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from PaymentIntent p where p.publicId = :publicId")
	Optional<PaymentIntent> findForUpdateByPublicId(@Param("publicId") String publicId);
}
