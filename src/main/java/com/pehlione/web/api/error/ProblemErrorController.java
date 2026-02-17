package com.pehlione.web.api.error;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

import org.slf4j.MDC;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.error.ErrorAttributeOptions.Include;
import org.springframework.boot.webmvc.error.ErrorAttributes;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "Error", description = "Framework error fallback endpoint")
@RestController
public class ProblemErrorController implements ErrorController {

	private static final MediaType PROBLEM_JSON = MediaType.valueOf("application/problem+json");
	private final ErrorAttributes errorAttributes;

	public ProblemErrorController(ErrorAttributes errorAttributes) {
		this.errorAttributes = errorAttributes;
	}

	@RequestMapping("${server.error.path:/error}")
	public ResponseEntity<ProblemDetail> error(HttpServletRequest request) {
		Map<String, Object> attrs = errorAttributes.getErrorAttributes(
				new ServletWebRequest(request),
				ErrorAttributeOptions.of(Include.MESSAGE));

		int status = statusValue(attrs.get("status"));
		String path = String.valueOf(attrs.getOrDefault("path", request.getRequestURI()));
		String message = String.valueOf(attrs.getOrDefault("message", "Request failed"));

		HttpStatus httpStatus = HttpStatus.resolve(status);
		if (httpStatus == null) {
			httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
		}

		ApiErrorCode code = statusToCode(httpStatus);
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(httpStatus, detailFor(httpStatus, message));
		pd.setType(statusToType(httpStatus));
		pd.setTitle(httpStatus.getReasonPhrase());
		pd.setInstance(toUri(path));
		pd.setProperty("code", code.name());
		pd.setProperty("timestamp", Instant.now().toString());

		String requestId = requestId(request);
		if (requestId != null) {
			pd.setProperty("requestId", requestId);
		}

		return ResponseEntity.status(httpStatus).contentType(PROBLEM_JSON).body(pd);
	}

	private ApiErrorCode statusToCode(HttpStatus status) {
		return switch (status) {
			case BAD_REQUEST -> ApiErrorCode.VALIDATION_FAILED;
			case UNAUTHORIZED -> ApiErrorCode.UNAUTHORIZED;
			case FORBIDDEN -> ApiErrorCode.FORBIDDEN;
			case NOT_FOUND -> ApiErrorCode.NOT_FOUND;
			case CONFLICT -> ApiErrorCode.CONFLICT;
			case TOO_MANY_REQUESTS -> ApiErrorCode.RATE_LIMITED;
			default -> ApiErrorCode.INTERNAL_ERROR;
		};
	}

	private URI statusToType(HttpStatus status) {
		return switch (status) {
			case BAD_REQUEST -> ProblemTypes.BAD_REQUEST;
			case UNAUTHORIZED -> ProblemTypes.UNAUTHORIZED;
			case FORBIDDEN -> ProblemTypes.FORBIDDEN;
			case NOT_FOUND -> ProblemTypes.NOT_FOUND;
			case CONFLICT -> ProblemTypes.CONFLICT;
			case TOO_MANY_REQUESTS -> ProblemTypes.RATE_LIMITED;
			default -> ProblemTypes.INTERNAL;
		};
	}

	private String detailFor(HttpStatus status, String message) {
		if (status == HttpStatus.NOT_FOUND) {
			return "Not Found";
		}
		return (message == null || message.isBlank()) ? status.getReasonPhrase() : message;
	}

	private String requestId(HttpServletRequest request) {
		String fromHeader = request.getHeader("X-Request-Id");
		if (fromHeader != null && !fromHeader.isBlank()) {
			return fromHeader;
		}
		String fromMdc = MDC.get("requestId");
		return (fromMdc == null || fromMdc.isBlank()) ? null : fromMdc;
	}

	private URI toUri(String path) {
		try {
			return URI.create(path);
		} catch (Exception ex) {
			return URI.create("/error");
		}
	}

	private int statusValue(Object rawStatus) {
		if (rawStatus instanceof Number number) {
			return number.intValue();
		}
		if (rawStatus instanceof HttpStatusCode statusCode) {
			return statusCode.value();
		}
		if (rawStatus != null) {
			try {
				return Integer.parseInt(rawStatus.toString());
			} catch (NumberFormatException ignore) {
				// fallback below
			}
		}
		return 500;
	}
}
