package com.pehlione.web.payment;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payment intent status")
public enum PaymentStatus {
	REQUIRES_CONFIRMATION,
	SUCCEEDED,
	FAILED,
	EXPIRED,
	CANCELLED
}
