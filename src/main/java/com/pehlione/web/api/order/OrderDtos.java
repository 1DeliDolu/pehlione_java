package com.pehlione.web.api.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.pehlione.web.fulfillment.Shipment;
import com.pehlione.web.order.Order;
import com.pehlione.web.order.OrderItem;
import com.pehlione.web.order.OrderStatus;

public class OrderDtos {

	public record OrderSummaryResponse(
			String orderId,
			OrderStatus status,
			String currency,
			BigDecimal totalAmount,
			Instant createdAt) {
		static OrderSummaryResponse from(Order order) {
			return new OrderSummaryResponse(
					order.getPublicId(),
					order.getStatus(),
					order.getCurrency(),
					order.getTotalAmount(),
					order.getCreatedAt());
		}
	}

	public record OrderItemResponse(
			Long productId,
			String sku,
			String name,
			BigDecimal unitPrice,
			String currency,
			int quantity,
			BigDecimal lineTotal) {
		static OrderItemResponse from(OrderItem item) {
			Long productId = item.getProduct() == null ? null : item.getProduct().getId();
			return new OrderItemResponse(
					productId,
					item.getSku(),
					item.getName(),
					item.getUnitPrice(),
					item.getCurrency(),
					item.getQuantity(),
					item.getLineTotal());
		}
	}

	public record ShipmentInfo(
			String shipmentId,
			String status,
			String carrier,
			String trackingNumber,
			Instant shippedAt,
			Instant deliveredAt) {
		static ShipmentInfo from(Shipment shipment) {
			return new ShipmentInfo(
					shipment.getPublicId(),
					shipment.getStatus().name(),
					shipment.getCarrier(),
					shipment.getTrackingNumber(),
					shipment.getShippedAt(),
					shipment.getDeliveredAt());
		}
	}

	public record ShippingAddressInfo(
			String fullName,
			String phone,
			String line1,
			String line2,
			String city,
			String state,
			String postalCode,
			String countryCode) {
	}

	public record OrderDetailResponse(
			String orderId,
			OrderStatus status,
			String currency,
			BigDecimal totalAmount,
			Instant createdAt,
			List<OrderItemResponse> items,
			List<ShipmentInfo> shipments,
			ShippingAddressInfo shippingAddress) {
		static OrderDetailResponse from(Order order) {
			var sa = order.getShippingAddress();
			ShippingAddressInfo shippingAddress = sa == null ? null : new ShippingAddressInfo(
					sa.getFullName(),
					sa.getPhone(),
					sa.getLine1(),
					sa.getLine2(),
					sa.getCity(),
					sa.getState(),
					sa.getPostalCode(),
					sa.getCountryCode());
			return new OrderDetailResponse(
					order.getPublicId(),
					order.getStatus(),
					order.getCurrency(),
					order.getTotalAmount(),
					order.getCreatedAt(),
					order.getItems().stream().map(OrderItemResponse::from).toList(),
					order.getShipments().stream().map(ShipmentInfo::from).toList(),
					shippingAddress);
		}
	}
}
