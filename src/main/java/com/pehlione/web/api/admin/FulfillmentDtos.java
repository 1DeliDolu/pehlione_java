package com.pehlione.web.api.admin;

import java.time.Instant;

import jakarta.validation.constraints.Size;

public class FulfillmentDtos {

	public record ShipRequest(
			@Size(max = 64) String carrier,
			@Size(max = 128) String trackingNumber) {
	}

	public record ShipmentResponse(
			String shipmentId,
			String status,
			String carrier,
			String trackingNumber,
			Instant shippedAt,
			Instant deliveredAt) {
	}

	public record CancelRequest(@Size(max = 255) String reason) {
	}
}
