package com.pehlione.web.payment;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Refund processing status")
public enum RefundStatus {
	PENDING,
	SUCCEEDED,
	FAILED,
	CANCELLED
}
