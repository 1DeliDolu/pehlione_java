package com.pehlione.web.api.payment;

import java.math.BigDecimal;
import java.time.Instant;

import com.pehlione.web.payment.PaymentIntent;
import com.pehlione.web.payment.PaymentProvider;
import com.pehlione.web.payment.PaymentStatus;

public class PaymentDtos {

	public record PaymentResponse(
			String paymentId,
			PaymentProvider provider,
			PaymentStatus status,
			String currency,
			BigDecimal amount,
			String orderId,
			Instant createdAt,
			Instant updatedAt) {
		public static PaymentResponse from(PaymentIntent pi) {
			return new PaymentResponse(
					pi.getPublicId(),
					pi.getProvider(),
					pi.getStatus(),
					pi.getCurrency(),
					pi.getAmount(),
					pi.getOrder().getPublicId(),
					pi.getCreatedAt(),
					pi.getUpdatedAt());
		}
	}

	public record FailRequest(String error) {
	}
}
