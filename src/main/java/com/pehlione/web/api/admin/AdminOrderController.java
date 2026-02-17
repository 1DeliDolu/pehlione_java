package com.pehlione.web.api.admin;

import static com.pehlione.web.api.admin.AdminOrderDtos.AdminOrderDetail;
import static com.pehlione.web.api.admin.AdminOrderDtos.AdminOrderSummary;

import java.time.Instant;

import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pehlione.web.api.common.PageMapper;
import com.pehlione.web.api.common.PageResponse;
import com.pehlione.web.api.error.ApiErrorCode;
import com.pehlione.web.api.error.ApiException;
import com.pehlione.web.order.Order;
import com.pehlione.web.order.OrderRepository;
import com.pehlione.web.order.OrderSpecifications;
import com.pehlione.web.order.OrderStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Admin - Orders", description = "Administrative order management endpoints")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderController {

	private final OrderRepository repo;

	public AdminOrderController(OrderRepository repo) {
		this.repo = repo;
	}

	@Operation(summary = "Admin: list orders", description = "Filter by status/email/orderId/date range.")
	@GetMapping
	@Transactional(readOnly = true)
	public PageResponse<AdminOrderSummary> list(
			@Parameter(description = "Order status filter")
			@RequestParam(name = "status", required = false) OrderStatus status,
			@Parameter(description = "User email contains", example = "gmail.com")
			@RequestParam(name = "email", required = false) String email,
			@Parameter(description = "Order id contains", example = "6b7a")
			@RequestParam(required = false, name = "q") String query,
			@Parameter(description = "Created-at lower bound (ISO date-time)", example = "2026-02-01T00:00:00Z")
			@RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
			@Parameter(description = "Created-at upper bound (ISO date-time)", example = "2026-02-28T23:59:59Z")
			@RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
			@ParameterObject Pageable pageable) {
		var spec = OrderSpecifications.statusEq(status)
				.and(OrderSpecifications.userEmailLike(email))
				.and(OrderSpecifications.orderIdLike(query))
				.and(OrderSpecifications.createdFrom(from))
				.and(OrderSpecifications.createdTo(to));
		return PageMapper.of(repo.findAll(spec, pageable).map(AdminOrderSummary::from));
	}

	@GetMapping("/{orderId}")
	@Transactional(readOnly = true)
	public AdminOrderDetail get(@PathVariable("orderId") String orderId) {
		Order order = repo.findByPublicId(orderId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Order not found"));
		order.getShipments().size();
		return AdminOrderDetail.from(order);
	}
}
