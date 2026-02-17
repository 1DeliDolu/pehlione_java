package com.pehlione.web.api.error;

import org.springframework.http.HttpStatus;

public class RateLimitExceededException extends ApiException {

	private final long retryAfterSeconds;

	public RateLimitExceededException(long retryAfterSeconds) {
		super(HttpStatus.TOO_MANY_REQUESTS, ApiErrorCode.RATE_LIMITED, "Too many requests");
		this.retryAfterSeconds = Math.max(1L, retryAfterSeconds);
	}

	public long getRetryAfterSeconds() {
		return retryAfterSeconds;
	}
}
