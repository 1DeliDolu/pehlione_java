package com.pehlione.web.api.admin;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pehlione.web.webhook.PaymentWebhookEvent;
import com.pehlione.web.webhook.PaymentWebhookEventRepository;
import com.pehlione.web.webhook.WebhookEventSpecifications;

@RestController
@RequestMapping("/api/v1/admin/webhook-events")
public class AdminWebhookEventController {

	private final PaymentWebhookEventRepository repo;

	public AdminWebhookEventController(PaymentWebhookEventRepository repo) {
		this.repo = repo;
	}

	public record WebhookEventRow(
			String provider,
			String eventId,
			String payloadHash,
			Instant receivedAt) {
		static WebhookEventRow from(PaymentWebhookEvent event) {
			return new WebhookEventRow(
					event.getProvider(),
					event.getEventId(),
					event.getPayloadHash(),
					event.getReceivedAt());
		}
	}

	@GetMapping
	public Page<WebhookEventRow> list(
			@RequestParam(required = false) String provider,
			@RequestParam(required = false) String eventId,
			Pageable pageable) {
		var spec = WebhookEventSpecifications.providerEq(provider)
				.and(WebhookEventSpecifications.eventIdLike(eventId));
		return repo.findAll(spec, pageable).map(WebhookEventRow::from);
	}
}
