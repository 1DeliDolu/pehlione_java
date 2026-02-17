package com.pehlione.web.order;

import java.time.Instant;

import org.springframework.data.jpa.domain.Specification;

public final class OrderSpecifications {

	private OrderSpecifications() {
	}

	public static Specification<Order> statusEq(OrderStatus status) {
		return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
	}

	public static Specification<Order> userEmailLike(String email) {
		return (root, query, cb) -> {
			if (email == null || email.isBlank()) {
				return cb.conjunction();
			}
			var userJoin = root.join("user");
			return cb.like(cb.lower(userJoin.get("email")), "%" + email.trim().toLowerCase() + "%");
		};
	}

	public static Specification<Order> orderIdLike(String qstr) {
		return (root, query, cb) -> {
			if (qstr == null || qstr.isBlank()) {
				return cb.conjunction();
			}
			String needle = "%" + qstr.trim().toLowerCase() + "%";
			return cb.like(cb.lower(root.get("publicId")), needle);
		};
	}

	public static Specification<Order> createdFrom(Instant from) {
		return (root, query, cb) -> from == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("createdAt"), from);
	}

	public static Specification<Order> createdTo(Instant to) {
		return (root, query, cb) -> to == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("createdAt"), to);
	}
}
