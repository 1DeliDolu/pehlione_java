package com.pehlione.web.payment;

import java.time.Instant;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pehlione.web.api.error.ApiErrorCode;
import com.pehlione.web.api.error.ApiException;
import com.pehlione.web.audit.AuditService;
import com.pehlione.web.auth.ClientInfo;
import com.pehlione.web.checkout.OrderDraftRepository;
import com.pehlione.web.checkout.OrderDraftStatus;
import com.pehlione.web.inventory.InventoryService;
import com.pehlione.web.notification.OrderPaidEvent;
import com.pehlione.web.notification.PaymentFailedEvent;
import com.pehlione.web.notification.RefundRequestedEvent;
import com.pehlione.web.order.Order;
import com.pehlione.web.order.OrderItem;
import com.pehlione.web.order.OrderRepository;
import com.pehlione.web.order.OrderStatus;
import com.pehlione.web.order.OrderTransitionService;
import com.pehlione.web.user.User;

@Service
public class PaymentService {

	private final PaymentIntentRepository repo;
	private final OrderRepository orderRepo;
	private final InventoryService inventoryService;
	private final AuditService auditService;
	private final OrderDraftRepository draftRepo;
	private final RefundRepository refundRepo;
	private final OrderTransitionService transitionService;
	private final ApplicationEventPublisher eventPublisher;

	public PaymentService(
			PaymentIntentRepository repo,
			OrderRepository orderRepo,
			InventoryService inventoryService,
			AuditService auditService,
			OrderDraftRepository draftRepo,
			RefundRepository refundRepo,
			OrderTransitionService transitionService,
			ApplicationEventPublisher eventPublisher) {
		this.repo = repo;
		this.orderRepo = orderRepo;
		this.inventoryService = inventoryService;
		this.auditService = auditService;
		this.draftRepo = draftRepo;
		this.refundRepo = refundRepo;
		this.transitionService = transitionService;
		this.eventPublisher = eventPublisher;
	}

	@Transactional
	public PaymentIntent createIntent(User user, Order order, String idempotencyKey) {
		String cleanKey = normalizeKey(idempotencyKey);
		if (cleanKey != null) {
			var existing = repo.findByIdempotencyKey(cleanKey);
			if (existing.isPresent()) {
				PaymentIntent intent = existing.get();
				if (!intent.getUser().getId().equals(user.getId()) || !intent.getOrder().getId().equals(order.getId())) {
					throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "Idempotency key already used");
				}
				return intent;
			}
		}

		PaymentIntent pi = new PaymentIntent();
		pi.setPublicId(UUID.randomUUID().toString());
		pi.setUser(user);
		pi.setOrder(order);
		pi.setProvider(PaymentProvider.MOCK);
		pi.setStatus(PaymentStatus.REQUIRES_CONFIRMATION);
		pi.setCurrency(order.getCurrency());
		pi.setAmount(order.getTotalAmount());
		pi.setIdempotencyKey(cleanKey);

		PaymentIntent saved = repo.save(pi);
		auditService.record(
				user,
				"PAYMENT_INTENT_CREATED",
				"PAYMENT",
				saved.getPublicId(),
				null,
				"order=" + order.getPublicId() + " amount=" + saved.getAmount() + " " + saved.getCurrency());
		return saved;
	}

	@Transactional(readOnly = true)
	public java.util.Optional<PaymentIntent> findByIdempotencyKey(String idempotencyKey) {
		String cleanKey = normalizeKey(idempotencyKey);
		if (cleanKey == null) {
			return java.util.Optional.empty();
		}
		return repo.findByIdempotencyKey(cleanKey);
	}

	@Transactional
	public PaymentIntent getForUser(Long userId, String paymentPublicId) {
		PaymentIntent pi = repo.findByPublicId(paymentPublicId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Payment not found"));
		if (!pi.getUser().getId().equals(userId)) {
			throw new ApiException(HttpStatus.FORBIDDEN, ApiErrorCode.FORBIDDEN, "Not your payment");
		}
		return pi;
	}

	@Transactional
	public Refund createRefundMock(User user, Order order, String reason) {
		if (!order.getUser().getId().equals(user.getId())) {
			throw new ApiException(HttpStatus.FORBIDDEN, ApiErrorCode.FORBIDDEN, "Not your order");
		}
		if (order.getStatus() == OrderStatus.REFUNDED || order.getStatus() == OrderStatus.REFUND_PENDING) {
			throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "Refund already requested");
		}
		if (order.getStatus() != OrderStatus.PAID) {
			throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "Order must be PAID to refund");
		}

		Refund latest = refundRepo.findTopByOrderIdOrderByCreatedAtDesc(order.getId()).orElse(null);
		if (latest != null && latest.getStatus() == RefundStatus.PENDING) {
			return latest;
		}

		Refund refund = new Refund();
		refund.setPublicId(UUID.randomUUID().toString());
		refund.setUser(user);
		refund.setOrder(order);
		refund.setPaymentIntent(repo.findTopByOrderIdOrderByIdDesc(order.getId()).orElse(null));
		refund.setProvider(PaymentProvider.MOCK);
		refund.setStatus(RefundStatus.PENDING);
		refund.setCurrency(order.getCurrency());
		refund.setAmount(order.getTotalAmount());
		refund.setReason(truncReason(reason));

		transitionService.transition(order, OrderStatus.REFUND_PENDING, "user-refund-request");
		Refund saved = refundRepo.save(refund);
		auditService.record(
				user,
				"REFUND_CREATED",
				"REFUND",
				saved.getPublicId(),
				null,
				"order=" + order.getPublicId() + " amount=" + saved.getAmount() + " " + saved.getCurrency());
		eventPublisher.publishEvent(RefundRequestedEvent.from(saved, Instant.now()));
		return saved;
	}

	@Transactional
	public void confirmMock(String userEmail, String paymentPublicId, ClientInfo client) {
		PaymentIntent pi = repo.findForUpdateByPublicId(paymentPublicId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Payment not found"));

		if (pi.getStatus() == PaymentStatus.SUCCEEDED) {
			return;
		}
		if (pi.getStatus() != PaymentStatus.REQUIRES_CONFIRMATION) {
			throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "Payment not confirmable");
		}

		Order order = orderRepo.findForUpdateWithItems(pi.getOrder().getId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Order not found"));
		if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
			throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "Order is not pending payment");
		}

		for (OrderItem item : order.getItems()) {
			if (item.getReservationPublicId() != null) {
				inventoryService.consume(userEmail, item.getReservationPublicId());
			}
		}

		transitionService.transition(order, OrderStatus.PAID, "payment-confirm-mock");
		pi.setStatus(PaymentStatus.SUCCEEDED);
		pi.setProviderReference("mock_" + pi.getPublicId());

		if (order.getSourceDraftPublicId() != null) {
			draftRepo.findForUpdateByPublicIdAndUserId(order.getSourceDraftPublicId(), pi.getUser().getId())
					.ifPresent(d -> d.setStatus(OrderDraftStatus.SUBMITTED));
		}

		auditService.record(
				pi.getUser(),
				"PAYMENT_SUCCEEDED",
				"PAYMENT",
				pi.getPublicId(),
				client,
				"order=" + order.getPublicId());
		eventPublisher.publishEvent(OrderPaidEvent.from(order, pi, Instant.now()));
	}

	@Transactional
	public void failMock(Long userId, String paymentPublicId, String error) {
		PaymentIntent pi = repo.findForUpdateByPublicId(paymentPublicId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Payment not found"));
		if (!pi.getUser().getId().equals(userId)) {
			throw new ApiException(HttpStatus.FORBIDDEN, ApiErrorCode.FORBIDDEN, "Not your payment");
		}
		if (pi.getStatus() == PaymentStatus.SUCCEEDED) {
			throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "Already paid");
		}

		Order order = orderRepo.findForUpdateWithItems(pi.getOrder().getId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Order not found"));
		if (order.getStatus() == OrderStatus.PAID) {
			throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "Already paid");
		}

		pi.setStatus(PaymentStatus.FAILED);
		pi.setLastError(trunc(error));
		transitionService.transition(order, OrderStatus.PAYMENT_FAILED, "payment-fail-mock");
		if (order.getSourceDraftPublicId() != null) {
			draftRepo.findForUpdateByPublicIdAndUserId(order.getSourceDraftPublicId(), pi.getUser().getId())
					.ifPresent(d -> d.setStatus(OrderDraftStatus.CANCELLED));
		}
		eventPublisher.publishEvent(PaymentFailedEvent.from(order, pi, pi.getLastError(), Instant.now()));
	}

	private String normalizeKey(String key) {
		if (key == null) {
			return null;
		}
		String trimmed = key.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private String trunc(String s) {
		if (s == null) {
			return null;
		}
		String t = s.trim();
		return t.length() > 2000 ? t.substring(0, 2000) : t;
	}

	private String truncReason(String s) {
		if (s == null) {
			return null;
		}
		String t = s.trim();
		if (t.isEmpty()) {
			return null;
		}
		return t.length() > 255 ? t.substring(0, 255) : t;
	}
}
