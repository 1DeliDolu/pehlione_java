package com.pehlione.web.webhook;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pehlione.web.api.error.ApiErrorCode;
import com.pehlione.web.api.error.ApiException;
import com.pehlione.web.checkout.OrderDraftRepository;
import com.pehlione.web.checkout.OrderDraftStatus;
import com.pehlione.web.inventory.InventoryService;
import com.pehlione.web.order.Order;
import com.pehlione.web.order.OrderItem;
import com.pehlione.web.order.OrderRepository;
import com.pehlione.web.order.OrderStatus;
import com.pehlione.web.order.OrderTransitionService;
import com.pehlione.web.payment.PaymentIntent;
import com.pehlione.web.payment.PaymentIntentRepository;
import com.pehlione.web.payment.PaymentStatus;
import com.pehlione.web.payment.Refund;
import com.pehlione.web.payment.RefundRepository;
import com.pehlione.web.payment.RefundStatus;

@Service
public class PaymentWebhookService {

	private static final String PROVIDER = "MOCK";

	private final PaymentWebhookEventRepository eventRepo;
	private final PaymentIntentRepository paymentRepo;
	private final OrderRepository orderRepo;
	private final RefundRepository refundRepo;
	private final InventoryService inventoryService;
	private final OrderDraftRepository draftRepo;
	private final OrderTransitionService transitionService;

	public PaymentWebhookService(
			PaymentWebhookEventRepository eventRepo,
			PaymentIntentRepository paymentRepo,
			OrderRepository orderRepo,
			RefundRepository refundRepo,
			InventoryService inventoryService,
			OrderDraftRepository draftRepo,
			OrderTransitionService transitionService) {
		this.eventRepo = eventRepo;
		this.paymentRepo = paymentRepo;
		this.orderRepo = orderRepo;
		this.refundRepo = refundRepo;
		this.inventoryService = inventoryService;
		this.draftRepo = draftRepo;
		this.transitionService = transitionService;
	}

	public record WebhookEvent(
			String eventId,
			String type,
			String paymentId,
			String orderId,
			String refundId,
			String error) {
	}

	@Transactional
	public void handleMock(String rawPayload, WebhookEvent event) {
		if (event == null || isBlank(event.eventId()) || isBlank(event.type())) {
			throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, "eventId and type are required");
		}

		String hash = WebhookSignatureVerifier.sha256Hex(rawPayload);
		var existing = eventRepo.findByProviderAndEventId(PROVIDER, event.eventId());
		if (existing.isPresent()) {
			if (!hash.equals(existing.get().getPayloadHash())) {
				throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "Webhook event payload mismatch");
			}
			return;
		}

		try {
			PaymentWebhookEvent we = new PaymentWebhookEvent();
			we.setProvider(PROVIDER);
			we.setEventId(event.eventId());
			we.setPayloadHash(hash);
			eventRepo.save(we);
		} catch (DataIntegrityViolationException ex) {
			PaymentWebhookEvent raced = eventRepo.findByProviderAndEventId(PROVIDER, event.eventId()).orElse(null);
			if (raced != null && hash.equals(raced.getPayloadHash())) {
				return;
			}
			throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "Webhook event already processed");
		}

		switch (event.type()) {
			case "payment.succeeded" -> onPaymentSucceeded(event.paymentId(), event.orderId());
			case "payment.failed" -> onPaymentFailed(event.paymentId(), event.orderId(), event.error());
			case "refund.succeeded" -> onRefundSucceeded(event.refundId());
			case "refund.failed" -> onRefundFailed(event.refundId(), event.error());
			default -> {
			}
		}
	}

	private void onPaymentSucceeded(String paymentId, String orderPublicId) {
		if (isBlank(paymentId)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, "paymentId is required");
		}

		PaymentIntent pi = paymentRepo.findForUpdateByPublicId(paymentId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Payment not found"));
		if (pi.getStatus() == PaymentStatus.SUCCEEDED) {
			return;
		}
		if (pi.getStatus() != PaymentStatus.REQUIRES_CONFIRMATION) {
			throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "Payment not confirmable");
		}

		String resolvedOrderId = isBlank(orderPublicId) ? pi.getOrder().getPublicId() : orderPublicId;
		Order order = orderRepo.findForUpdateByPublicId(resolvedOrderId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Order not found"));

		if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
			throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "Order is not pending payment");
		}

		for (OrderItem item : order.getItems()) {
			if (item.getReservationPublicId() == null) {
				continue;
			}
			try {
				inventoryService.systemConsume(item.getReservationPublicId(), "webhook-payment-succeeded");
			} catch (ApiException ex) {
				if (ex.getStatus() == HttpStatus.CONFLICT) {
					transitionService.transition(order, OrderStatus.PAYMENT_FAILED, "webhook-payment-succeeded-expired");
					pi.setStatus(PaymentStatus.FAILED);
					pi.setLastError(ex.getMessage());
					moveDraftToCancelled(order, pi.getUser().getId());
					return;
				}
				throw ex;
			}
		}

		transitionService.transition(order, OrderStatus.PAID, "webhook-payment-succeeded");
		pi.setStatus(PaymentStatus.SUCCEEDED);
		if (isBlank(pi.getProviderReference())) {
			pi.setProviderReference("mock_" + UUID.randomUUID());
		}
		moveDraftToSubmitted(order, pi.getUser().getId());
	}

	private void onPaymentFailed(String paymentId, String orderPublicId, String error) {
		if (isBlank(paymentId)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, "paymentId is required");
		}

		PaymentIntent pi = paymentRepo.findForUpdateByPublicId(paymentId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Payment not found"));
		if (pi.getStatus() == PaymentStatus.SUCCEEDED) {
			throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "Already paid");
		}

		String resolvedOrderId = isBlank(orderPublicId) ? pi.getOrder().getPublicId() : orderPublicId;
		Order order = orderRepo.findForUpdateByPublicId(resolvedOrderId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Order not found"));

		if (pi.getStatus() == PaymentStatus.FAILED && order.getStatus() == OrderStatus.PAYMENT_FAILED) {
			return;
		}
		if (order.getStatus() == OrderStatus.PAID) {
			throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "Already paid");
		}

		if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
			for (OrderItem item : order.getItems()) {
				if (item.getReservationPublicId() != null) {
					inventoryService.systemRelease(item.getReservationPublicId(), "webhook-payment-failed");
				}
			}
		}

		transitionService.transition(order, OrderStatus.PAYMENT_FAILED, "webhook-payment-failed");
		pi.setStatus(PaymentStatus.FAILED);
		pi.setLastError(trunc(error));
		moveDraftToCancelled(order, pi.getUser().getId());
	}

	private void onRefundSucceeded(String refundPublicId) {
		if (isBlank(refundPublicId)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, "refundId is required");
		}

		Refund refund = refundRepo.findForUpdateByPublicId(refundPublicId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Refund not found"));
		if (refund.getStatus() == RefundStatus.SUCCEEDED) {
			return;
		}

		Order order = orderRepo.findForUpdateByPublicId(refund.getOrder().getPublicId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Order not found"));
		refund.setStatus(RefundStatus.SUCCEEDED);
		refund.setLastError(null);
		if (isBlank(refund.getProviderReference())) {
			refund.setProviderReference("mock_ref_" + UUID.randomUUID());
		}
		transitionService.transition(order, OrderStatus.REFUNDED, "webhook-refund-succeeded");
	}

	private void onRefundFailed(String refundPublicId, String error) {
		if (isBlank(refundPublicId)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, "refundId is required");
		}

		Refund refund = refundRepo.findForUpdateByPublicId(refundPublicId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Refund not found"));
		if (refund.getStatus() == RefundStatus.FAILED) {
			return;
		}

		Order order = orderRepo.findForUpdateByPublicId(refund.getOrder().getPublicId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Order not found"));

		refund.setStatus(RefundStatus.FAILED);
		refund.setLastError(trunc(error));
		if (order.getStatus() == OrderStatus.REFUND_PENDING) {
			transitionService.transition(order, OrderStatus.PAID, "webhook-refund-failed");
		}
	}

	private void moveDraftToSubmitted(Order order, Long userId) {
		if (order.getSourceDraftPublicId() == null) {
			return;
		}
		draftRepo.findForUpdateByPublicIdAndUserId(order.getSourceDraftPublicId(), userId)
				.ifPresent(d -> d.setStatus(OrderDraftStatus.SUBMITTED));
	}

	private void moveDraftToCancelled(Order order, Long userId) {
		if (order.getSourceDraftPublicId() == null) {
			return;
		}
		draftRepo.findForUpdateByPublicIdAndUserId(order.getSourceDraftPublicId(), userId)
				.ifPresent(d -> d.setStatus(OrderDraftStatus.CANCELLED));
	}

	private boolean isBlank(String s) {
		return s == null || s.isBlank();
	}

	private String trunc(String s) {
		if (s == null) {
			return null;
		}
		String t = s.trim();
		return t.length() > 2000 ? t.substring(0, 2000) : t;
	}
}
