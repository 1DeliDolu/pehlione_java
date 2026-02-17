package com.pehlione.web.fulfillment;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Shipment lifecycle status")
public enum ShipmentStatus {
	CREATED,
	SHIPPED,
	DELIVERED,
	CANCELLED
}
