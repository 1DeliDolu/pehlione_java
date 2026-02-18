package com.pehlione.web.notification;

import java.math.BigDecimal;
import java.time.Instant;

import com.pehlione.web.payment.Refund;

public record RefundSucceededEvent(
		String userEmail,
		String orderId,
		String refundId,
		BigDecimal amount,
		String currency,
		Instant succeededAt) {

	public static RefundSucceededEvent from(Refund refund, Instant succeededAt) {
		return new RefundSucceededEvent(
				refund == null || refund.getUser() == null ? null : refund.getUser().getEmail(),
				refund == null || refund.getOrder() == null ? null : refund.getOrder().getPublicId(),
				refund == null ? null : refund.getPublicId(),
				refund == null ? null : refund.getAmount(),
				refund == null ? null : refund.getCurrency(),
				succeededAt == null ? Instant.now() : succeededAt);
	}
}
