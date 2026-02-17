package com.pehlione.web.api.order;

import static com.pehlione.web.api.order.OrderDtos.OrderDetailResponse;
import static com.pehlione.web.api.order.OrderDtos.OrderSummaryResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pehlione.web.api.error.ApiErrorCode;
import com.pehlione.web.api.error.ApiException;
import com.pehlione.web.order.OrderService;
import com.pehlione.web.payment.PaymentService;
import com.pehlione.web.user.UserRepository;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

	private final OrderService orderService;
	private final PaymentService paymentService;
	private final UserRepository userRepo;

	public OrderController(OrderService orderService, PaymentService paymentService, UserRepository userRepo) {
		this.orderService = orderService;
		this.paymentService = paymentService;
		this.userRepo = userRepo;
	}

	@GetMapping
	public Page<OrderSummaryResponse> list(@AuthenticationPrincipal Jwt jwt, Pageable pageable) {
		Long userId = userRepo.findByEmail(jwt.getSubject())
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ApiErrorCode.UNAUTHORIZED, "User not found"))
				.getId();
		return orderService.listForUser(userId, pageable).map(OrderSummaryResponse::from);
	}

	@GetMapping("/{orderId}")
	public OrderDetailResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable("orderId") String orderId) {
		Long userId = userRepo.findByEmail(jwt.getSubject())
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ApiErrorCode.UNAUTHORIZED, "User not found"))
				.getId();
		return OrderDetailResponse.from(orderService.getForUser(orderId, userId));
	}

	@PostMapping("/{orderId}/refund")
	public OrderRefundDtos.RefundResponse refund(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("orderId") String orderId,
			@RequestBody(required = false) OrderRefundDtos.RefundRequest req) {
		var user = userRepo.findByEmail(jwt.getSubject())
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ApiErrorCode.UNAUTHORIZED, "User not found"));
		var order = orderService.getForUser(orderId, user.getId());
		var refund = paymentService.createRefundMock(user, order, req == null ? null : req.reason());
		return new OrderRefundDtos.RefundResponse(refund.getPublicId());
	}
}
