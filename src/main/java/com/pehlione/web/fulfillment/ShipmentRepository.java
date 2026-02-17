package com.pehlione.web.fulfillment;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

	List<Shipment> findByOrderIdOrderByIdAsc(Long orderId);

	Optional<Shipment> findByPublicId(String publicId);
}
