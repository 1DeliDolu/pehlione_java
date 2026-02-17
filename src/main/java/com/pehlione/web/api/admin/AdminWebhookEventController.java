package com.pehlione.web.api.admin;

import java.time.Instant;

import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pehlione.web.api.common.PageMapper;
import com.pehlione.web.api.common.PageResponse;
import com.pehlione.web.webhook.PaymentWebhookEvent;
import com.pehlione.web.webhook.PaymentWebhookEventRepository;
import com.pehlione.web.webhook.WebhookEventSpecifications;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Admin - Webhook Events", description = "Administrative webhook event query endpoints")
@SecurityRequirement(name = "bearerAuth")
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

	@Operation(summary = "Admin: list webhook events", description = "Filter events by provider and event id.")
	@GetMapping
	public PageResponse<WebhookEventRow> list(
			@Parameter(description = "Payment provider", example = "mock")
			@RequestParam(name = "provider", required = false) String provider,
			@Parameter(description = "Event id contains", example = "evt_")
			@RequestParam(name = "eventId", required = false) String eventId,
			@ParameterObject Pageable pageable) {
		var spec = WebhookEventSpecifications.providerEq(provider)
				.and(WebhookEventSpecifications.eventIdLike(eventId));
		return PageMapper.of(repo.findAll(spec, pageable).map(WebhookEventRow::from));
	}
}
