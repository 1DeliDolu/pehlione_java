package com.pehlione.web.api.admin;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pehlione.web.payment.Refund;
import com.pehlione.web.payment.RefundRepository;
import com.pehlione.web.payment.RefundSpecifications;
import com.pehlione.web.payment.RefundStatus;

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

	@GetMapping
	@Transactional(readOnly = true)
	public Page<RefundSummary> list(
			@RequestParam(required = false) RefundStatus status,
			@RequestParam(required = false) String email,
			@RequestParam(required = false) String orderId,
			Pageable pageable) {
		var spec = RefundSpecifications.statusEq(status)
				.and(RefundSpecifications.userEmailLike(email))
				.and(RefundSpecifications.orderIdEq(orderId));
		return repo.findAll(spec, pageable).map(RefundSummary::from);
	}
}
