package com.pehlione.web.webhook;

import org.springframework.data.jpa.domain.Specification;

public final class WebhookEventSpecifications {

	private WebhookEventSpecifications() {
	}

	public static Specification<PaymentWebhookEvent> providerEq(String provider) {
		return (root, query, cb) -> {
			if (provider == null || provider.isBlank()) {
				return cb.conjunction();
			}
			return cb.equal(root.get("provider"), provider.trim());
		};
	}

	public static Specification<PaymentWebhookEvent> eventIdLike(String eventId) {
		return (root, query, cb) -> {
			if (eventId == null || eventId.isBlank()) {
				return cb.conjunction();
			}
			return cb.like(cb.lower(root.get("eventId")), "%" + eventId.trim().toLowerCase() + "%");
		};
	}
}
