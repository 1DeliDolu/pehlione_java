package com.pehlione.web.notification;

import java.time.Instant;

import com.pehlione.web.order.Order;
import com.pehlione.web.payment.PaymentIntent;

public record PaymentFailedEvent(
		String userEmail,
		String orderId,
		String paymentId,
		String reason,
		Instant failedAt) {

	public static PaymentFailedEvent from(Order order, PaymentIntent payment, String reason, Instant failedAt) {
		return new PaymentFailedEvent(
				order == null || order.getUser() == null ? null : order.getUser().getEmail(),
				order == null ? null : order.getPublicId(),
				payment == null ? null : payment.getPublicId(),
				reason,
				failedAt == null ? Instant.now() : failedAt);
	}
}
