package com.pehlione.web.api.order;

import static com.pehlione.web.api.order.OrderDtos.OrderDetailResponse;
import static com.pehlione.web.api.order.OrderDtos.OrderSummaryResponse;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pehlione.web.api.common.PageMapper;
import com.pehlione.web.api.common.PageResponse;
import com.pehlione.web.api.error.ApiErrorCode;
import com.pehlione.web.api.error.ApiException;
import com.pehlione.web.order.OrderService;
import com.pehlione.web.payment.PaymentService;
import com.pehlione.web.user.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Orders", description = "User order endpoints")
@SecurityRequirement(name = "bearerAuth")
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

	@Operation(
			summary = "List my orders",
			description = "Returns the authenticated user's orders (newest first).")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Paged order list"),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(
					mediaType = "application/problem+json",
					schema = @Schema(ref = "#/components/schemas/ApiProblem")))
	})
	@GetMapping
	public PageResponse<OrderSummaryResponse> list(
			@AuthenticationPrincipal Jwt jwt,
			@ParameterObject Pageable pageable) {
		Long userId = userRepo.findByEmail(jwt.getSubject())
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ApiErrorCode.UNAUTHORIZED, "User not found"))
				.getId();
		return PageMapper.of(orderService.listForUser(userId, pageable).map(OrderSummaryResponse::from));
	}

	@Operation(summary = "Get my order details")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Order detail"),
			@ApiResponse(responseCode = "404", description = "Order not found", content = @Content(
					mediaType = "application/problem+json",
					schema = @Schema(ref = "#/components/schemas/ApiProblem")))
	})
	@GetMapping("/{orderId}")
	public OrderDetailResponse get(
			@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Order public id", example = "6b7a2f42-0b0a-4d88-9129-6de8da1a6f10")
			@PathVariable("orderId") String orderId) {
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
