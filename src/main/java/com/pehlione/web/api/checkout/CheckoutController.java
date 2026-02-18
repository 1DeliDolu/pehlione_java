package com.pehlione.web.api.checkout;

import static com.pehlione.web.api.checkout.CheckoutDtos.DraftResponse;
import static com.pehlione.web.api.checkout.CheckoutDtos.PayRequest;
import static com.pehlione.web.api.checkout.CheckoutDtos.ReserveRequest;
import static com.pehlione.web.api.checkout.CheckoutDtos.StartPaymentResponse;
import static com.pehlione.web.api.checkout.CheckoutDtos.SubmitResponse;

import java.time.YearMonth;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pehlione.web.api.error.ApiErrorCode;
import com.pehlione.web.api.error.ApiException;
import com.pehlione.web.checkout.CheckoutService;
import com.pehlione.web.auth.ClientInfo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@Tag(name = "Checkout", description = "Checkout draft and payment flow endpoints")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/checkout")
public class CheckoutController {

	private final CheckoutService checkoutService;

	public CheckoutController(CheckoutService checkoutService) {
		this.checkoutService = checkoutService;
	}

	@PostMapping("/reserve")
	public DraftResponse reserve(@AuthenticationPrincipal Jwt jwt, @RequestBody(required = false) ReserveRequest req) {
		Integer ttl = req == null ? null : req.ttlMinutes();
		var draft = checkoutService.reserveFromCart(jwt.getSubject(), ttl);
		return DraftResponse.from(draft);
	}

	@GetMapping("/drafts/{draftId}")
	public DraftResponse getDraft(@AuthenticationPrincipal Jwt jwt, @PathVariable("draftId") String draftId) {
		return DraftResponse.from(checkoutService.getDraft(jwt.getSubject(), draftId));
	}

	@PostMapping("/drafts/{draftId}/cancel")
	public void cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable("draftId") String draftId) {
		checkoutService.cancel(jwt.getSubject(), draftId);
	}

	@PostMapping("/drafts/{draftId}/submit")
	public SubmitResponse submit(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("draftId") String draftId,
			HttpServletRequest request) {
		String sid = jwt.getClaimAsString("sid");
		var order = checkoutService.submit(jwt.getSubject(), sid, draftId, ClientInfo.from(request));
		return new SubmitResponse(order.getPublicId());
	}

	@Operation(
			summary = "Start payment for a draft",
			description = "Creates an order in PENDING_PAYMENT, snapshots shipping address and creates payment intent. "
					+ "Supports Idempotency-Key header.")
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "Payment started",
					content = @Content(
							mediaType = "application/json",
							schema = @Schema(implementation = StartPaymentResponse.class),
							examples = @ExampleObject(value = """
									{"paymentId":"9f6e2a4f","orderId":"2a1b9d6c"}
									"""))),
			@ApiResponse(
					responseCode = "400",
					description = "Validation error",
					content = @Content(
							mediaType = "application/problem+json",
							schema = @Schema(ref = "#/components/schemas/ApiProblem"))),
			@ApiResponse(
					responseCode = "409",
					description = "Draft not reserved/expired",
					content = @Content(
							mediaType = "application/problem+json",
							schema = @Schema(ref = "#/components/schemas/ApiProblem")))
	})
	@PostMapping("/drafts/{draftId}/pay")
	public StartPaymentResponse pay(
			@AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Draft public id", example = "drf_1a2b3c4d")
			@PathVariable("draftId") String draftId,
			@Valid @RequestBody PayRequest req,
			HttpServletRequest request,
			@Parameter(description = "Idempotency key for safe retries", example = "checkout-42")
			@RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
		validateCardFieldsIfProvided(req);
		String sid = jwt.getClaimAsString("sid");
		var result = checkoutService.startPayment(
				jwt.getSubject(),
				sid,
				draftId,
				ClientInfo.from(request),
				idempotencyKey,
				req.addressId());
		return new StartPaymentResponse(result.paymentId(), result.orderId());
	}

	private void validateCardFieldsIfProvided(PayRequest req) {
		boolean anyCardFieldProvided =
				!isBlank(req.cardHolderName())
						|| !isBlank(req.cardNumber())
						|| req.expiryMonth() != null
						|| req.expiryYear() != null
						|| !isBlank(req.cvc());
		if (!anyCardFieldProvided) {
			return;
		}

		if (isBlank(req.cardHolderName()) || isBlank(req.cardNumber()) || req.expiryMonth() == null
				|| req.expiryYear() == null || isBlank(req.cvc())) {
			throw validation("All card fields are required when card data is provided");
		}

		String normalizedCardNumber = req.cardNumber().replaceAll("[\\s-]", "");
		if (!normalizedCardNumber.matches("\\d{13,19}")) {
			throw validation("cardNumber must be 13-19 digits");
		}

		int expiryMonth = req.expiryMonth();
		if (expiryMonth < 1 || expiryMonth > 12) {
			throw validation("expiryMonth must be between 1 and 12");
		}

		int expiryYear = req.expiryYear();
		YearMonth now = YearMonth.now();
		if (expiryYear < now.getYear() || expiryYear > now.getYear() + 30) {
			throw validation("expiryYear is out of allowed range");
		}

		YearMonth expiry = YearMonth.of(expiryYear, expiryMonth);
		if (expiry.isBefore(now)) {
			throw validation("Card is expired");
		}

		String normalizedCvc = req.cvc().replaceAll("\\s+", "");
		if (!normalizedCvc.matches("\\d{3,4}")) {
			throw validation("cvc must be 3-4 digits");
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	private ApiException validation(String message) {
		return new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, message);
	}
}
