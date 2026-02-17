package com.pehlione.web.api.admin;

import static com.pehlione.web.api.admin.FulfillmentDtos.CancelRequest;
import static com.pehlione.web.api.admin.FulfillmentDtos.ShipRequest;
import static com.pehlione.web.api.admin.FulfillmentDtos.ShipmentResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pehlione.web.api.error.ApiErrorCode;
import com.pehlione.web.api.error.ApiException;
import com.pehlione.web.auth.ClientInfo;
import com.pehlione.web.fulfillment.FulfillmentService;
import com.pehlione.web.fulfillment.Shipment;
import com.pehlione.web.user.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminFulfillmentController {

	private final FulfillmentService service;
	private final UserRepository userRepo;

	public AdminFulfillmentController(FulfillmentService service, UserRepository userRepo) {
		this.service = service;
		this.userRepo = userRepo;
	}

	@PostMapping("/{orderId}/ship")
	public ShipmentResponse ship(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("orderId") String orderId,
			@Valid @RequestBody ShipRequest req,
			HttpServletRequest request) {
		var admin = userRepo.findByEmail(jwt.getSubject())
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ApiErrorCode.UNAUTHORIZED, "User not found"));
		Shipment shipment = service.ship(admin, orderId, req.carrier(), req.trackingNumber(), ClientInfo.from(request));
		return new ShipmentResponse(
				shipment.getPublicId(),
				shipment.getStatus().name(),
				shipment.getCarrier(),
				shipment.getTrackingNumber(),
				shipment.getShippedAt(),
				shipment.getDeliveredAt());
	}

	@PostMapping("/{orderId}/deliver")
	public void deliver(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("orderId") String orderId,
			HttpServletRequest request) {
		var admin = userRepo.findByEmail(jwt.getSubject())
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ApiErrorCode.UNAUTHORIZED, "User not found"));
		service.deliver(admin, orderId, ClientInfo.from(request));
	}

	@PostMapping("/{orderId}/cancel")
	public void cancel(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("orderId") String orderId,
			@RequestBody(required = false) CancelRequest req,
			HttpServletRequest request) {
		var admin = userRepo.findByEmail(jwt.getSubject())
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ApiErrorCode.UNAUTHORIZED, "User not found"));
		String reason = req == null ? null : req.reason();
		service.cancel(admin, orderId, reason, ClientInfo.from(request));
	}
}
