package com.pehlione.web.api.webhook;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pehlione.web.api.error.ApiErrorCode;
import com.pehlione.web.api.error.ApiException;
import com.pehlione.web.webhook.PaymentWebhookService;
import com.pehlione.web.webhook.WebhookSignatureVerifier;

import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/v1/webhooks/mock-payment")
public class MockPaymentWebhookController {

	private static final JsonMapper JSON = new JsonMapper();

	private final String secret;
	private final PaymentWebhookService service;

	public MockPaymentWebhookController(
			@Value("${app.webhooks.mock.secret}") String secret,
			PaymentWebhookService service) {
		this.secret = secret;
		this.service = service;
	}

	@PostMapping
	public void handle(
			@RequestHeader(name = "X-Signature", required = false) String signature,
			@RequestBody String rawBody) throws Exception {
		if (!WebhookSignatureVerifier.verifyHmacSha256Hex(secret, rawBody, signature)) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, ApiErrorCode.UNAUTHORIZED, "Invalid signature");
		}

		var node = JSON.readTree(rawBody);
		var event = new PaymentWebhookService.WebhookEvent(
				node.path("eventId").asText(),
				node.path("type").asText(),
				node.path("paymentId").asText(null),
				node.path("orderId").asText(null),
				node.path("refundId").asText(null),
				node.path("error").asText(null));

		service.handleMock(rawBody, event);
	}
}
