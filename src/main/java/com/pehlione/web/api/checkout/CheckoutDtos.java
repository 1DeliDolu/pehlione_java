package com.pehlione.web.api.checkout;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.pehlione.web.checkout.OrderDraft;
import com.pehlione.web.checkout.OrderDraftItem;
import com.pehlione.web.checkout.OrderDraftStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public class CheckoutDtos {

	public record ReserveRequest(Integer ttlMinutes) {
	}

	public record DraftItemResponse(
			Long productId,
			String sku,
			String name,
			BigDecimal unitPrice,
			String currency,
			int quantity,
			BigDecimal lineTotal,
			String reservationId,
			Instant reservationExpiresAt) {
		static DraftItemResponse from(OrderDraftItem it) {
			return new DraftItemResponse(
					it.getProduct().getId(),
					it.getSku(),
					it.getName(),
					it.getUnitPrice(),
					it.getCurrency(),
					it.getQuantity(),
					it.getLineTotal(),
					it.getReservationPublicId(),
					it.getReservationExpiresAt());
		}
	}

	public record DraftResponse(
			String draftId,
			OrderDraftStatus status,
			String currency,
			BigDecimal totalAmount,
			Instant expiresAt,
			List<DraftItemResponse> items) {
		public static DraftResponse from(OrderDraft d) {
			return new DraftResponse(
					d.getPublicId(),
					d.getStatus(),
					d.getCurrency(),
					d.getTotalAmount(),
					d.getExpiresAt(),
					d.getItems().stream().map(DraftItemResponse::from).toList());
		}
	}

	public record SubmitResponse(String orderId) {
	}

	public record StartPaymentResponse(String paymentId, String orderId) {
	}

	public record PayRequest(
			@Schema(
					description = "Shipping address id",
					example = "1",
					requiredMode = Schema.RequiredMode.REQUIRED)
			@NotNull Long addressId) {
	}
}
