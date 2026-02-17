package com.pehlione.web.webhook;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, Long>,
		JpaSpecificationExecutor<PaymentWebhookEvent> {

	Optional<PaymentWebhookEvent> findByProviderAndEventId(String provider, String eventId);
}
