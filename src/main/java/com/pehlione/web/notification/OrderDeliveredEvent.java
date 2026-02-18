package com.pehlione.web.notification;

import java.time.Instant;

import com.pehlione.web.fulfillment.Shipment;
import com.pehlione.web.order.Order;

public record OrderDeliveredEvent(
		String userEmail,
		String orderId,
		String shipmentId,
		Instant deliveredAt) {

	public static OrderDeliveredEvent from(Order order, Shipment shipment, Instant deliveredAt) {
		return new OrderDeliveredEvent(
				order == null || order.getUser() == null ? null : order.getUser().getEmail(),
				order == null ? null : order.getPublicId(),
				shipment == null ? null : shipment.getPublicId(),
				deliveredAt == null ? Instant.now() : deliveredAt);
	}
}
