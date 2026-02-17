package com.pehlione.web.api.cart;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CartDtos {

	public record UpsertCartItemRequest(@NotNull Long productId, @Min(1) int quantity) {
	}

	public record CartItemResponse(
			Long productId,
			String sku,
			String name,
			BigDecimal unitPrice,
			String currency,
			int quantity,
			BigDecimal lineTotal,
			String primaryImageUrl) {
	}

	public record CartResponse(List<CartItemResponse> items) {
	}
}
