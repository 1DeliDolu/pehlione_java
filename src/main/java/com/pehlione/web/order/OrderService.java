package com.pehlione.web.order;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pehlione.web.api.error.ApiErrorCode;
import com.pehlione.web.api.error.ApiException;
import com.pehlione.web.audit.AuditService;
import com.pehlione.web.auth.ClientInfo;
import com.pehlione.web.checkout.OrderDraft;
import com.pehlione.web.checkout.OrderDraftItem;
import com.pehlione.web.user.User;

@Service
public class OrderService {

	private final OrderRepository repo;
	private final AuditService auditService;

	public OrderService(OrderRepository repo, AuditService auditService) {
		this.repo = repo;
		this.auditService = auditService;
	}

	@Transactional
	public Order createFromDraft(User user, OrderDraft draft, String sid, ClientInfo client) {
		return createFromDraftWithStatus(user, draft, sid, client, OrderStatus.PLACED);
	}

	@Transactional
	public Order createFromDraftWithStatus(
			User user,
			OrderDraft draft,
			String sid,
			ClientInfo client,
			OrderStatus status) {
		Order order = new Order();
		order.setPublicId(UUID.randomUUID().toString());
		order.setUser(user);
		order.setStatus(status);
		order.setCurrency(draft.getCurrency());
		order.setTotalAmount(draft.getTotalAmount());
		order.setCreatedBySid(sid);
		order.setSourceDraftPublicId(draft.getPublicId());

		for (OrderDraftItem draftItem : draft.getItems()) {
			OrderItem item = new OrderItem();
			item.setOrder(order);
			item.setProduct(draftItem.getProduct());
			item.setSku(draftItem.getSku());
			item.setName(draftItem.getName());
			item.setUnitPrice(draftItem.getUnitPrice());
			item.setCurrency(draftItem.getCurrency());
			item.setQuantity(draftItem.getQuantity());
			item.setLineTotal(draftItem.getLineTotal());
			item.setReservationPublicId(draftItem.getReservationPublicId());
			order.getItems().add(item);
		}

		Order saved = repo.save(order);
		auditService.record(
				user,
				"ORDER_CREATED",
				"ORDER",
				saved.getPublicId(),
				client,
				"status=" + saved.getStatus() + " total=" + saved.getTotalAmount() + " " + saved.getCurrency());
		return saved;
	}

	@Transactional(readOnly = true)
	public Page<Order> listForUser(Long userId, Pageable pageable) {
		return repo.findByUserIdOrderByCreatedAtDesc(userId, pageable);
	}

	@Transactional(readOnly = true)
	public Order getForUser(String publicId, Long userId) {
		Order order = repo.findByPublicIdAndUserId(publicId, userId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Order not found"));
		order.getItems().size();
		order.getShipments().size();
		if (order.getShippingAddress() != null) {
			order.getShippingAddress().getFullName();
		}
		return order;
	}
}
