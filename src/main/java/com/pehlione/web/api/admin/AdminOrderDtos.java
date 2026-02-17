package com.pehlione.web.api.admin;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.pehlione.web.api.order.OrderDtos;
import com.pehlione.web.order.Order;
import com.pehlione.web.order.OrderStatus;

public class AdminOrderDtos {

	public record AdminOrderSummary(
			String orderId,
			OrderStatus status,
			String userEmail,
			String currency,
			BigDecimal totalAmount,
			Instant createdAt) {
		public static AdminOrderSummary from(Order order) {
			return new AdminOrderSummary(
					order.getPublicId(),
					order.getStatus(),
					order.getUser().getEmail(),
					order.getCurrency(),
					order.getTotalAmount(),
					order.getCreatedAt());
		}
	}

	public record AdminOrderDetail(
			String orderId,
			OrderStatus status,
			String userEmail,
			String currency,
			BigDecimal totalAmount,
			Instant createdAt,
			OrderDtos.ShippingAddressInfo shippingAddress,
			List<OrderDtos.OrderItemResponse> items,
			List<OrderDtos.ShipmentInfo> shipments) {
		public static AdminOrderDetail from(Order order) {
			var sa = order.getShippingAddress();
			OrderDtos.ShippingAddressInfo shippingAddress = sa == null ? null : new OrderDtos.ShippingAddressInfo(
					sa.getFullName(),
					sa.getPhone(),
					sa.getLine1(),
					sa.getLine2(),
					sa.getCity(),
					sa.getState(),
					sa.getPostalCode(),
					sa.getCountryCode());

			List<OrderDtos.OrderItemResponse> items = order.getItems().stream()
					.map(i -> new OrderDtos.OrderItemResponse(
							i.getProduct() == null ? null : i.getProduct().getId(),
							i.getSku(),
							i.getName(),
							i.getUnitPrice(),
							i.getCurrency(),
							i.getQuantity(),
							i.getLineTotal()))
					.toList();

			List<OrderDtos.ShipmentInfo> shipments = order.getShipments().stream()
					.map(s -> new OrderDtos.ShipmentInfo(
							s.getPublicId(),
							s.getStatus().name(),
							s.getCarrier(),
							s.getTrackingNumber(),
							s.getShippedAt(),
							s.getDeliveredAt()))
					.toList();

			return new AdminOrderDetail(
					order.getPublicId(),
					order.getStatus(),
					order.getUser().getEmail(),
					order.getCurrency(),
					order.getTotalAmount(),
					order.getCreatedAt(),
					shippingAddress,
					items,
					shipments);
		}
	}
}
