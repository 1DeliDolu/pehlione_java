package com.pehlione.web.api.admin;

import static com.pehlione.web.api.admin.AdminOrderDtos.AdminOrderDetail;
import static com.pehlione.web.api.admin.AdminOrderDtos.AdminOrderSummary;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pehlione.web.api.error.ApiErrorCode;
import com.pehlione.web.api.error.ApiException;
import com.pehlione.web.order.Order;
import com.pehlione.web.order.OrderRepository;
import com.pehlione.web.order.OrderSpecifications;
import com.pehlione.web.order.OrderStatus;

@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderController {

	private final OrderRepository repo;

	public AdminOrderController(OrderRepository repo) {
		this.repo = repo;
	}

	@GetMapping
	@Transactional(readOnly = true)
	public Page<AdminOrderSummary> list(
			@RequestParam(required = false) OrderStatus status,
			@RequestParam(required = false) String email,
			@RequestParam(required = false, name = "q") String query,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
			Pageable pageable) {
		var spec = OrderSpecifications.statusEq(status)
				.and(OrderSpecifications.userEmailLike(email))
				.and(OrderSpecifications.orderIdLike(query))
				.and(OrderSpecifications.createdFrom(from))
				.and(OrderSpecifications.createdTo(to));
		return repo.findAll(spec, pageable).map(AdminOrderSummary::from);
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
