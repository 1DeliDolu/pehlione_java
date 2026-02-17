package com.pehlione.web.inventory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {

	Optional<InventoryReservation> findByPublicId(String publicId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from InventoryReservation r where r.publicId = :publicId")
	Optional<InventoryReservation> findByPublicIdForUpdate(@Param("publicId") String publicId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select r from InventoryReservation r
			where r.status = :status
			and r.expiresAt <= :now
			""")
	List<InventoryReservation> lockByStatusAndExpiresBefore(
			@Param("status") ReservationStatus status,
			@Param("now") Instant now);
}
