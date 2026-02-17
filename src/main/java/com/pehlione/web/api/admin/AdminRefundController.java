package com.pehlione.web.api.admin;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pehlione.web.api.common.PageMapper;
import com.pehlione.web.api.common.PageResponse;
import com.pehlione.web.payment.Refund;
import com.pehlione.web.payment.RefundRepository;
import com.pehlione.web.payment.RefundSpecifications;
import com.pehlione.web.payment.RefundStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Admin - Refunds", description = "Administrative refund listing endpoints")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/admin/refunds")
public class AdminRefundController {

	private final RefundRepository repo;

	public AdminRefundController(RefundRepository repo) {
		this.repo = repo;
	}

	public record RefundSummary(
			String refundId,
			RefundStatus status,
			String userEmail,
			String orderId,
			String currency,
			BigDecimal amount,
			Instant createdAt) {
		static RefundSummary from(Refund refund) {
			return new RefundSummary(
					refund.getPublicId(),
					refund.getStatus(),
					refund.getUser().getEmail(),
					refund.getOrder().getPublicId(),
					refund.getCurrency(),
					refund.getAmount(),
					refund.getCreatedAt());
		}
	}

	@Operation(summary = "Admin: list refunds", description = "Filter by status, user email and order id.")
	@GetMapping
	@Transactional(readOnly = true)
	public PageResponse<RefundSummary> list(
			@Parameter(description = "Refund status filter")
			@RequestParam(name = "status", required = false) RefundStatus status,
			@Parameter(description = "User email contains", example = "gmail.com")
			@RequestParam(name = "email", required = false) String email,
			@Parameter(description = "Order id exact match", example = "6b7a2f42")
			@RequestParam(name = "orderId", required = false) String orderId,
			@ParameterObject Pageable pageable) {
		var spec = RefundSpecifications.statusEq(status)
				.and(RefundSpecifications.userEmailLike(email))
				.and(RefundSpecifications.orderIdEq(orderId));
		return PageMapper.of(repo.findAll(spec, pageable).map(RefundSummary::from));
	}
}
