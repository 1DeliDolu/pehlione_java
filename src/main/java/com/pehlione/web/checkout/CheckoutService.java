package com.pehlione.web.checkout;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pehlione.web.address.AddressService;
import com.pehlione.web.api.error.ApiErrorCode;
import com.pehlione.web.api.error.ApiException;
import com.pehlione.web.auth.ClientInfo;
import com.pehlione.web.cart.CartItem;
import com.pehlione.web.cart.CartItemRepository;
import com.pehlione.web.inventory.InventoryReservationRepository;
import com.pehlione.web.inventory.InventoryService;
import com.pehlione.web.order.Order;
import com.pehlione.web.order.OrderAddressService;
import com.pehlione.web.order.OrderService;
import com.pehlione.web.order.OrderStatus;
import com.pehlione.web.payment.PaymentService;
import com.pehlione.web.product.Product;
import com.pehlione.web.product.ProductRepository;
import com.pehlione.web.product.ProductStatus;
import com.pehlione.web.user.User;
import com.pehlione.web.user.UserRepository;

@Service
public class CheckoutService {

	private final UserRepository userRepo;
	private final CartItemRepository cartRepo;
	private final ProductRepository productRepo;
	private final InventoryService inventoryService;
	private final InventoryReservationRepository reservationRepo;
	private final OrderDraftRepository draftRepo;
	private final OrderService orderService;
	private final PaymentService paymentService;
	private final AddressService addressService;
	private final OrderAddressService orderAddressService;

	public CheckoutService(
			UserRepository userRepo,
			CartItemRepository cartRepo,
			ProductRepository productRepo,
			InventoryService inventoryService,
			InventoryReservationRepository reservationRepo,
			OrderDraftRepository draftRepo,
			OrderService orderService,
			PaymentService paymentService,
			AddressService addressService,
			OrderAddressService orderAddressService) {
		this.userRepo = userRepo;
		this.cartRepo = cartRepo;
		this.productRepo = productRepo;
		this.inventoryService = inventoryService;
		this.reservationRepo = reservationRepo;
		this.draftRepo = draftRepo;
		this.orderService = orderService;
		this.paymentService = paymentService;
		this.addressService = addressService;
		this.orderAddressService = orderAddressService;
	}

	@Transactional
	public OrderDraft reserveFromCart(String userEmail, Integer ttlMinutesNullable) {
		int ttl = ttlMinutesNullable == null ? 15 : ttlMinutesNullable;
		if (ttl <= 0 || ttl > 120) {
			ttl = 15;
		}

		User user = userRepo.findByEmail(userEmail)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ApiErrorCode.UNAUTHORIZED, "User not found"));

		List<CartItem> cart = cartRepo.findByUserId(user.getId());
		if (cart.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, "Cart is empty");
		}

		OrderDraft draft = new OrderDraft();
		draft.setPublicId(UUID.randomUUID().toString());
		draft.setUser(user);
		draft.setStatus(OrderDraftStatus.DRAFT);

		String currency = null;
		BigDecimal total = BigDecimal.ZERO;
		Instant minExpiry = null;

		for (CartItem cartItem : cart) {
			Long productId = cartItem.getProduct().getId();
			int quantity = cartItem.getQuantity();

			Product product = productRepo.findById(productId)
					.orElseThrow(() -> new ApiException(
							HttpStatus.NOT_FOUND,
							ApiErrorCode.NOT_FOUND,
							"Product not found: " + productId));

			if (product.getStatus() != ProductStatus.ACTIVE) {
				throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "Product not active: " + product.getId());
			}

			if (currency == null) {
				currency = product.getCurrency();
			}
			if (!currency.equals(product.getCurrency())) {
				throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, "Mixed currencies not supported");
			}

			var reservationResult = inventoryService.reserve(userEmail, product.getId(), quantity, ttl);
			var reservation = reservationRepo.findByPublicId(reservationResult.reservationId()).orElse(null);

			BigDecimal unitPrice = product.getPrice();
			BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);

			OrderDraftItem item = new OrderDraftItem();
			item.setDraft(draft);
			item.setProduct(product);
			item.setSku(product.getSku());
			item.setName(product.getName());
			item.setUnitPrice(unitPrice);
			item.setCurrency(product.getCurrency());
			item.setQuantity(quantity);
			item.setLineTotal(lineTotal);
			item.setReservation(reservation);
			item.setReservationPublicId(reservationResult.reservationId());
			item.setReservationExpiresAt(reservationResult.expiresAt());
			draft.getItems().add(item);

			total = total.add(lineTotal);
			if (minExpiry == null || reservationResult.expiresAt().isBefore(minExpiry)) {
				minExpiry = reservationResult.expiresAt();
			}
		}

		draft.setCurrency(currency == null ? "EUR" : currency);
		draft.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
		draft.setExpiresAt(minExpiry);
		draft.setStatus(OrderDraftStatus.RESERVED);
		draftRepo.save(draft);

		cartRepo.deleteAllByUserId(user.getId());
		return draft;
	}

	@Transactional(readOnly = true)
	public OrderDraft getDraft(String userEmail, String draftPublicId) {
		User user = userRepo.findByEmail(userEmail)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ApiErrorCode.UNAUTHORIZED, "User not found"));
		return draftRepo.findDetailsByPublicIdAndUserId(draftPublicId, user.getId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Draft not found"));
	}

	@Transactional
	public void cancel(String userEmail, String draftPublicId) {
		User user = userRepo.findByEmail(userEmail)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ApiErrorCode.UNAUTHORIZED, "User not found"));

		OrderDraft draft = draftRepo.findForUpdateByPublicIdAndUserId(draftPublicId, user.getId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Draft not found"));

		if (draft.getStatus() == OrderDraftStatus.CANCELLED || draft.getStatus() == OrderDraftStatus.EXPIRED) {
			return;
		}
		if (draft.getStatus() == OrderDraftStatus.SUBMITTED) {
			throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "Already submitted");
		}
		if (draft.getStatus() == OrderDraftStatus.PAYMENT_PENDING) {
			throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "Payment already started");
		}

		for (OrderDraftItem item : draft.getItems()) {
			if (item.getReservationPublicId() != null) {
				inventoryService.release(userEmail, item.getReservationPublicId());
			}
		}
		draft.setStatus(OrderDraftStatus.CANCELLED);
	}

	@Transactional
	public Order submit(String userEmail, String sid, String draftPublicId, ClientInfo client) {
		User user = userRepo.findByEmail(userEmail)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ApiErrorCode.UNAUTHORIZED, "User not found"));

		OrderDraft draft = draftRepo.findForUpdateByPublicIdAndUserId(draftPublicId, user.getId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Draft not found"));

		if (draft.getStatus() != OrderDraftStatus.RESERVED) {
			throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "Draft is not reserved");
		}
		if (draft.getExpiresAt() != null && draft.getExpiresAt().isBefore(Instant.now())) {
			draft.setStatus(OrderDraftStatus.EXPIRED);
			throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "Draft expired");
		}

		for (OrderDraftItem item : draft.getItems()) {
			if (item.getReservationPublicId() != null) {
				inventoryService.consume(userEmail, item.getReservationPublicId());
			}
		}

		Order order = orderService.createFromDraft(user, draft, sid, client);
		draft.setStatus(OrderDraftStatus.SUBMITTED);
		return order;
	}

	public record StartPaymentResult(String paymentId, String orderId) {
	}

	@Transactional
	public StartPaymentResult startPayment(
			String userEmail,
			String sid,
			String draftPublicId,
			ClientInfo client,
			String idempotencyKey,
			Long addressId) {
		User user = userRepo.findByEmail(userEmail)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ApiErrorCode.UNAUTHORIZED, "User not found"));

		OrderDraft draft = draftRepo.findForUpdateByPublicIdAndUserId(draftPublicId, user.getId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Draft not found"));

		if (addressId == null) {
			throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, "addressId is required");
		}

		if (draft.getStatus() == OrderDraftStatus.PAYMENT_PENDING) {
			var existing = paymentService.findByIdempotencyKey(idempotencyKey).orElse(null);
			if (existing != null
					&& existing.getUser().getId().equals(user.getId())
					&& existing.getOrder().getSourceDraftPublicId() != null
					&& existing.getOrder().getSourceDraftPublicId().equals(draft.getPublicId())) {
				return new StartPaymentResult(existing.getPublicId(), existing.getOrder().getPublicId());
			}
			throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "Draft payment already started");
		}

		if (draft.getStatus() != OrderDraftStatus.RESERVED) {
			throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "Draft is not reserved");
		}
		if (draft.getExpiresAt() != null && draft.getExpiresAt().isBefore(Instant.now())) {
			draft.setStatus(OrderDraftStatus.EXPIRED);
			throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "Draft expired");
		}

		var address = addressService.getForUserOrThrow(userEmail, addressId);
		Order order = orderService.createFromDraftWithStatus(user, draft, sid, client, OrderStatus.PENDING_PAYMENT);
		orderAddressService.attachShippingSnapshot(order, address);
		var payment = paymentService.createIntent(user, order, idempotencyKey);
		draft.setStatus(OrderDraftStatus.PAYMENT_PENDING);
		return new StartPaymentResult(payment.getPublicId(), order.getPublicId());
	}

	@Transactional
	public void expireDrafts() {
		List<OrderDraft> expired = draftRepo.findExpiredReserved(Instant.now());
		for (OrderDraft draft : expired) {
			if (draft.getStatus() == OrderDraftStatus.RESERVED) {
				draft.setStatus(OrderDraftStatus.EXPIRED);
			}
		}
	}
}
