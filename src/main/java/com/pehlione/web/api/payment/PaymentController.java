package com.pehlione.web.api.payment;

import static com.pehlione.web.api.payment.PaymentDtos.FailRequest;
import static com.pehlione.web.api.payment.PaymentDtos.PaymentResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pehlione.web.api.error.ApiErrorCode;
import com.pehlione.web.api.error.ApiException;
import com.pehlione.web.auth.ClientInfo;
import com.pehlione.web.payment.PaymentService;
import com.pehlione.web.user.UserRepository;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "Payments", description = "Payment lifecycle endpoints")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

	private final PaymentService paymentService;
	private final UserRepository userRepo;

	public PaymentController(PaymentService paymentService, UserRepository userRepo) {
		this.paymentService = paymentService;
		this.userRepo = userRepo;
	}

	@GetMapping("/{paymentId}")
	public PaymentResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable("paymentId") String paymentId) {
		Long userId = userRepo.findByEmail(jwt.getSubject())
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ApiErrorCode.UNAUTHORIZED, "User not found"))
				.getId();
		return PaymentResponse.from(paymentService.getForUser(userId, paymentId));
	}

	@PostMapping("/{paymentId}/confirm-mock")
	public void confirmMock(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("paymentId") String paymentId,
			HttpServletRequest request) {
		Long userId = userRepo.findByEmail(jwt.getSubject())
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ApiErrorCode.UNAUTHORIZED, "User not found"))
				.getId();
		paymentService.getForUser(userId, paymentId);
		paymentService.confirmMock(jwt.getSubject(), paymentId, ClientInfo.from(request));
	}

	@PostMapping("/{paymentId}/fail-mock")
	public void failMock(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("paymentId") String paymentId,
			@RequestBody(required = false) FailRequest req) {
		Long userId = userRepo.findByEmail(jwt.getSubject())
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ApiErrorCode.UNAUTHORIZED, "User not found"))
				.getId();
		String error = req == null ? "mock_failed" : req.error();
		paymentService.failMock(userId, paymentId, error);
	}
}
