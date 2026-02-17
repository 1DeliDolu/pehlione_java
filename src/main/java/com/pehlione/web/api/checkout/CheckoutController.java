package com.pehlione.web.api.checkout;

import static com.pehlione.web.api.checkout.CheckoutDtos.DraftResponse;
import static com.pehlione.web.api.checkout.CheckoutDtos.PayRequest;
import static com.pehlione.web.api.checkout.CheckoutDtos.ReserveRequest;
import static com.pehlione.web.api.checkout.CheckoutDtos.StartPaymentResponse;
import static com.pehlione.web.api.checkout.CheckoutDtos.SubmitResponse;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pehlione.web.checkout.CheckoutService;
import com.pehlione.web.auth.ClientInfo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

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

	@PostMapping("/drafts/{draftId}/pay")
	public StartPaymentResponse pay(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("draftId") String draftId,
			@Valid @RequestBody PayRequest req,
			HttpServletRequest request,
			@RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
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
}
