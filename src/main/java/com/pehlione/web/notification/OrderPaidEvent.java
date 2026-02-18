package com.pehlione.web.notification;

import java.math.BigDecimal;
import java.time.Instant;

import com.pehlione.web.order.Order;
import com.pehlione.web.payment.PaymentIntent;

public record OrderPaidEvent(
		String userEmail,
		String orderId,
		String paymentId,
		BigDecimal totalAmount,
		String currency,
		Instant paidAt) {

	public static OrderPaidEvent from(Order order, PaymentIntent payment, Instant paidAt) {
		return new OrderPaidEvent(
				order == null || order.getUser() == null ? null : order.getUser().getEmail(),
				order == null ? null : order.getPublicId(),
				payment == null ? null : payment.getPublicId(),
				order == null ? null : order.getTotalAmount(),
				order == null ? null : order.getCurrency(),
				paidAt == null ? Instant.now() : paidAt);
	}
}
