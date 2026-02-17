package com.pehlione.web.checkout;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface OrderDraftRepository extends JpaRepository<OrderDraft, Long> {

	@Query("""
			select distinct d from OrderDraft d
			left join fetch d.items i
			left join fetch i.product p
			where d.publicId = :publicId and d.user.id = :userId
			""")
	Optional<OrderDraft> findDetailsByPublicIdAndUserId(
			@Param("publicId") String publicId,
			@Param("userId") Long userId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select distinct d from OrderDraft d
			left join fetch d.items i
			left join fetch i.product p
			where d.publicId = :publicId and d.user.id = :userId
			""")
	Optional<OrderDraft> findForUpdateByPublicIdAndUserId(
			@Param("publicId") String publicId,
			@Param("userId") Long userId);

	List<OrderDraft> findByUserIdOrderByCreatedAtDesc(Long userId);

	@Query("""
			select d from OrderDraft d
			where d.status = com.pehlione.web.checkout.OrderDraftStatus.RESERVED
			and d.expiresAt is not null
			and d.expiresAt <= :now
			""")
	List<OrderDraft> findExpiredReserved(@Param("now") Instant now);
}
