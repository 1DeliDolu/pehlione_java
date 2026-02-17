package com.pehlione.web.api.order;

public class OrderRefundDtos {

	public record RefundRequest(String reason) {
	}

	public record RefundResponse(String refundId) {
	}
}
