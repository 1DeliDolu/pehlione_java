package com.pehlione.web.notification;

import java.time.Instant;

import com.pehlione.web.fulfillment.Shipment;
import com.pehlione.web.order.Order;

public record OrderShippedEvent(
		String userEmail,
		String orderId,
		String shipmentId,
		String carrier,
		String trackingNumber,
		Instant shippedAt) {

	public static OrderShippedEvent from(Order order, Shipment shipment, Instant shippedAt) {
		return new OrderShippedEvent(
				order == null || order.getUser() == null ? null : order.getUser().getEmail(),
				order == null ? null : order.getPublicId(),
				shipment == null ? null : shipment.getPublicId(),
				shipment == null ? null : shipment.getCarrier(),
				shipment == null ? null : shipment.getTrackingNumber(),
				shippedAt == null ? Instant.now() : shippedAt);
	}
}
