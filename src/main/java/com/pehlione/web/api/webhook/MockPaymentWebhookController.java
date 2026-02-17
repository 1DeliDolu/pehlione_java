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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import tools.jackson.databind.json.JsonMapper;

@Tag(name = "Webhooks", description = "Inbound webhook endpoints")
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

	@Operation(
			summary = "Mock payment webhook receiver",
			description = "Validates HMAC signature via X-Signature and processes the webhook event.",
			security = { @SecurityRequirement(name = "") })
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Accepted"),
			@ApiResponse(
					responseCode = "401",
					description = "Invalid signature",
					content = @Content(
							mediaType = "application/problem+json",
							schema = @Schema(ref = "#/components/schemas/ApiProblem")))
	})
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
