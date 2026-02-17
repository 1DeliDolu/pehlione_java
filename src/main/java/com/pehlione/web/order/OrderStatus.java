package com.pehlione.web.order;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Order status lifecycle")
public enum OrderStatus {
	PENDING_PAYMENT,
	PAID,
	SHIPPED,
	PAYMENT_FAILED,
	REFUND_PENDING,
	REFUNDED,
	PLACED,
	CANCELLED,
	FULFILLED
}
