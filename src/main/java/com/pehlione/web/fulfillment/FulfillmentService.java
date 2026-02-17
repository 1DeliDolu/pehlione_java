package com.pehlione.web.fulfillment;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pehlione.web.api.error.ApiErrorCode;
import com.pehlione.web.api.error.ApiException;
import com.pehlione.web.audit.AuditService;
import com.pehlione.web.auth.ClientInfo;
import com.pehlione.web.order.Order;
import com.pehlione.web.order.OrderRepository;
import com.pehlione.web.order.OrderStatus;
import com.pehlione.web.order.OrderTransitionService;
import com.pehlione.web.user.User;

@Service
public class FulfillmentService {

	private final OrderRepository orderRepo;
	private final ShipmentRepository shipmentRepo;
	private final AuditService auditService;
	private final OrderTransitionService transitionService;

	public FulfillmentService(
			OrderRepository orderRepo,
			ShipmentRepository shipmentRepo,
			AuditService auditService,
			OrderTransitionService transitionService) {
		this.orderRepo = orderRepo;
		this.shipmentRepo = shipmentRepo;
		this.auditService = auditService;
		this.transitionService = transitionService;
	}

	@Transactional
	public Shipment ship(User adminActor, String orderPublicId, String carrier, String trackingNumber, ClientInfo client) {
		Order order = orderRepo.findForUpdateByPublicId(orderPublicId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Order not found"));

		if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.FULFILLED) {
			throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "Order is not shippable");
		}
		if (order.getStatus() != OrderStatus.PAID) {
			throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "Order must be PAID to ship");
		}

		Shipment shipment = shipmentRepo.findByOrderIdOrderByIdAsc(order.getId()).stream()
				.findFirst()
				.orElseGet(() -> {
					Shipment created = new Shipment();
					created.setPublicId(UUID.randomUUID().toString());
					created.setOrder(order);
					created.setStatus(ShipmentStatus.CREATED);
					return created;
				});

		if (shipment.getStatus() == ShipmentStatus.DELIVERED) {
			throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "Already delivered");
		}
		if (shipment.getStatus() == ShipmentStatus.CANCELLED) {
			throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "Shipment cancelled");
		}

		shipment.setCarrier(trimOrNull(carrier));
		shipment.setTrackingNumber(trimOrNull(trackingNumber));
		shipment.setStatus(ShipmentStatus.SHIPPED);
		shipment.setShippedAt(Instant.now());
		shipmentRepo.save(shipment);

		transitionService.transition(order, OrderStatus.SHIPPED, "admin-ship");
		auditService.record(
				adminActor,
				"ORDER_SHIPPED",
				"ORDER",
				order.getPublicId(),
				client,
				"carrier=" + shipment.getCarrier() + " tracking=" + shipment.getTrackingNumber());
		return shipment;
	}

	@Transactional
	public void deliver(User adminActor, String orderPublicId, ClientInfo client) {
		Order order = orderRepo.findForUpdateByPublicId(orderPublicId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Order not found"));
		if (order.getStatus() != OrderStatus.SHIPPED) {
			throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "Order must be SHIPPED to deliver");
		}

		Shipment shipment = shipmentRepo.findByOrderIdOrderByIdAsc(order.getId()).stream()
				.findFirst()
				.orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "No shipment found"));
		if (shipment.getStatus() != ShipmentStatus.SHIPPED) {
			throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "Shipment must be SHIPPED to deliver");
		}

		shipment.setStatus(ShipmentStatus.DELIVERED);
		shipment.setDeliveredAt(Instant.now());
		transitionService.transition(order, OrderStatus.FULFILLED, "admin-deliver");

		auditService.record(
				adminActor,
				"ORDER_DELIVERED",
				"ORDER",
				order.getPublicId(),
				client,
				"delivered");
	}

	@Transactional
	public void cancel(User adminActor, String orderPublicId, String reason, ClientInfo client) {
		Order order = orderRepo.findForUpdateByPublicId(orderPublicId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Order not found"));

		if (order.getStatus() == OrderStatus.FULFILLED || order.getStatus() == OrderStatus.SHIPPED) {
			throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "Cannot cancel shipped/fulfilled order");
		}
		if (order.getStatus() == OrderStatus.CANCELLED) {
			return;
		}

		transitionService.transition(order, OrderStatus.CANCELLED, "admin-cancel");
		for (Shipment shipment : shipmentRepo.findByOrderIdOrderByIdAsc(order.getId())) {
			if (shipment.getStatus() != ShipmentStatus.DELIVERED) {
				shipment.setStatus(ShipmentStatus.CANCELLED);
			}
		}

		auditService.record(
				adminActor,
				"ORDER_CANCELLED",
				"ORDER",
				order.getPublicId(),
				client,
				"reason=" + trimOrNull(reason));
	}

	private String trimOrNull(String s) {
		if (s == null) {
			return null;
		}
		String trimmed = s.trim();
		return trimmed.isBlank() ? null : trimmed;
	}
}
