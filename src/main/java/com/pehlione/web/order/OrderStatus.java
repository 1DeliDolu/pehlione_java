package com.pehlione.web.order;

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
