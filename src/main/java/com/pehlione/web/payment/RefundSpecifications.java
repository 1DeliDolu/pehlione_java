package com.pehlione.web.payment;

import org.springframework.data.jpa.domain.Specification;

public final class RefundSpecifications {

	private RefundSpecifications() {
	}

	public static Specification<Refund> statusEq(RefundStatus status) {
		return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
	}

	public static Specification<Refund> orderIdEq(String orderPublicId) {
		return (root, query, cb) -> {
			if (orderPublicId == null || orderPublicId.isBlank()) {
				return cb.conjunction();
			}
			var orderJoin = root.join("order");
			return cb.equal(orderJoin.get("publicId"), orderPublicId.trim());
		};
	}

	public static Specification<Refund> userEmailLike(String email) {
		return (root, query, cb) -> {
			if (email == null || email.isBlank()) {
				return cb.conjunction();
			}
			var userJoin = root.join("user");
			return cb.like(cb.lower(userJoin.get("email")), "%" + email.trim().toLowerCase() + "%");
		};
	}
}
