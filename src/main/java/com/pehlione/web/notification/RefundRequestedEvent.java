package com.pehlione.web.notification;

import java.math.BigDecimal;
import java.time.Instant;

import com.pehlione.web.payment.Refund;

public record RefundRequestedEvent(
		String userEmail,
		String orderId,
		String refundId,
		BigDecimal amount,
		String currency,
		String reason,
		Instant requestedAt) {

	public static RefundRequestedEvent from(Refund refund, Instant requestedAt) {
		return new RefundRequestedEvent(
				refund == null || refund.getUser() == null ? null : refund.getUser().getEmail(),
				refund == null || refund.getOrder() == null ? null : refund.getOrder().getPublicId(),
				refund == null ? null : refund.getPublicId(),
				refund == null ? null : refund.getAmount(),
				refund == null ? null : refund.getCurrency(),
				refund == null ? null : refund.getReason(),
				requestedAt == null ? Instant.now() : requestedAt);
	}
}
