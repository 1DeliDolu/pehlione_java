package com.pehlione.web.order;

import java.util.EnumSet;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.pehlione.web.api.error.ApiErrorCode;
import com.pehlione.web.api.error.ApiException;

@Service
public class OrderTransitionService {

	private static final Map<OrderStatus, EnumSet<OrderStatus>> ALLOWED = Map.of(
			OrderStatus.PENDING_PAYMENT,
			EnumSet.of(OrderStatus.PAID, OrderStatus.PAYMENT_FAILED, OrderStatus.CANCELLED),
			OrderStatus.PAID,
			EnumSet.of(OrderStatus.SHIPPED, OrderStatus.REFUND_PENDING, OrderStatus.CANCELLED),
			OrderStatus.SHIPPED,
			EnumSet.of(OrderStatus.FULFILLED),
			OrderStatus.REFUND_PENDING,
			EnumSet.of(OrderStatus.REFUNDED, OrderStatus.PAID),
			OrderStatus.PAYMENT_FAILED,
			EnumSet.of(OrderStatus.CANCELLED),
			OrderStatus.CANCELLED,
			EnumSet.noneOf(OrderStatus.class),
			OrderStatus.FULFILLED,
			EnumSet.noneOf(OrderStatus.class),
			OrderStatus.REFUNDED,
			EnumSet.noneOf(OrderStatus.class),
			OrderStatus.PLACED,
			EnumSet.of(OrderStatus.SHIPPED, OrderStatus.REFUND_PENDING, OrderStatus.CANCELLED));

	public void assertTransition(OrderStatus from, OrderStatus to, String context) {
		if (from == to) {
			return;
		}
		EnumSet<OrderStatus> allowed = ALLOWED.get(from);
		if (allowed == null || !allowed.contains(to)) {
			String suffix = context == null ? "" : " (" + context + ")";
			throw new ApiException(
					HttpStatus.CONFLICT,
					ApiErrorCode.CONFLICT,
					"Invalid order transition: " + from + " -> " + to + suffix);
		}
	}

	public void transition(Order order, OrderStatus to, String context) {
		OrderStatus from = order.getStatus();
		if (from == to) {
			return;
		}
		assertTransition(from, to, context);
		order.setStatus(to);
	}
}
