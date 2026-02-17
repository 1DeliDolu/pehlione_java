package com.pehlione.web.api.inventory;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pehlione.web.inventory.InventoryService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

	private final InventoryService service;

	public InventoryController(InventoryService service) {
		this.service = service;
	}

	@PostMapping("/reserve")
	public ReservationResponse reserve(Authentication authentication, @Valid @RequestBody ReserveRequest req) {
		var res = service.reserve(authentication.getName(), req.productId(), req.quantity(), req.ttlMinutes());
		return new ReservationResponse(res.reservationId(), res.expiresAt().toString(), res.remainingStock());
	}

	@PostMapping("/reservations/{reservationId}/release")
	public void release(Authentication authentication, @PathVariable("reservationId") String reservationId) {
		service.release(authentication.getName(), reservationId);
	}

	@PostMapping("/reservations/{reservationId}/consume")
	public void consume(Authentication authentication, @PathVariable("reservationId") String reservationId) {
		service.consume(authentication.getName(), reservationId);
	}

	public record ReserveRequest(
			@NotNull Long productId,
			@Min(1) int quantity,
			@Min(1) Integer ttlMinutes) {
	}

	public record ReservationResponse(String reservationId, String expiresAt, int remainingStock) {
	}
}
