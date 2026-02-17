package com.pehlione.web.security.ratelimit;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.pehlione.web.api.error.RateLimitExceededException;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

	private final RateLimitProperties props;
	private final HandlerExceptionResolver exceptionResolver;
	private final Cache<String, Bucket> buckets;

	public RateLimitFilter(
			RateLimitProperties props,
			@Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
		this.props = props;
		this.exceptionResolver = exceptionResolver;
		int ttlMinutes = Math.max(1, props.getBucketTtlMinutes());
		this.buckets = Caffeine.newBuilder()
				.expireAfterAccess(ttlMinutes, TimeUnit.MINUTES)
				.maximumSize(200_000)
				.build();
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !props.isEnabled();
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		ResolvedPolicy policy = resolvePolicy(request);
		if (policy == null) {
			filterChain.doFilter(request, response);
			return;
		}

		Bucket bucket = buckets.get(policy.key(), k -> newBucket(policy.limits()));
		ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
		if (probe.isConsumed()) {
			response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
			filterChain.doFilter(request, response);
			return;
		}

		long retryAfterSeconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
		response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
		exceptionResolver.resolveException(
				request,
				response,
				null,
				new RateLimitExceededException(retryAfterSeconds));
	}

	private Bucket newBucket(List<LimitRule> limits) {
		var builder = Bucket.builder();
		for (LimitRule limit : limits) {
			int capacity = Math.max(1, limit.capacity());
			builder.addLimit(Bandwidth.classic(capacity, Refill.intervally(capacity, limit.period())));
		}
		return builder.build();
	}

	private ResolvedPolicy resolvePolicy(HttpServletRequest request) {
		String path = request.getRequestURI();
		if (path == null || path.isBlank()) {
			return null;
		}
		if (path.startsWith("/actuator/health") || "/actuator/info".equals(path)) {
			return null;
		}
		if (!path.startsWith("/api/v1/")) {
			return null;
		}

		Map<String, Integer> cfg = props.getPolicies();
		String ip = clientIp(request);
		String subject = currentSubject();

		if (path.startsWith("/api/v1/auth/")) {
			return new ResolvedPolicy(
					"auth_ip:" + ip,
					List.of(
							limit(pick(cfg, "auth_ip_per_minute", 10), Duration.ofMinutes(1)),
							limit(pick(cfg, "auth_ip_per_hour", 100), Duration.ofHours(1))));
		}

		if (path.startsWith("/api/v1/webhooks/")) {
			return new ResolvedPolicy(
					"webhook_ip:" + ip,
					List.of(limit(pick(cfg, "webhook_ip_per_minute", 120), Duration.ofMinutes(1))));
		}

		if (path.startsWith("/api/v1/checkout/")) {
			return userOrIp(
					subject,
					ip,
					"checkout_user",
					pick(cfg, "checkout_user_per_minute", 20),
					"checkout_ip",
					pick(cfg, "api_ip_per_minute", 300));
		}

		if (path.startsWith("/api/v1/payments/")) {
			return userOrIp(
					subject,
					ip,
					"payment_user",
					pick(cfg, "payment_user_per_minute", 20),
					"payment_ip",
					pick(cfg, "api_ip_per_minute", 300));
		}

		return userOrIp(
				subject,
				ip,
				"api_user",
				pick(cfg, "api_user_per_minute", 120),
				"api_ip",
				pick(cfg, "api_ip_per_minute", 300));
	}

	private ResolvedPolicy userOrIp(
			String subject,
			String ip,
			String userBucket,
			int userPerMinute,
			String ipBucket,
			int ipPerMinute) {
		if (subject != null && !subject.isBlank()) {
			return new ResolvedPolicy(
					userBucket + ":" + subject,
					List.of(limit(userPerMinute, Duration.ofMinutes(1))));
		}
		return new ResolvedPolicy(
				ipBucket + ":" + ip,
				List.of(limit(ipPerMinute, Duration.ofMinutes(1))));
	}

	private LimitRule limit(int capacity, Duration period) {
		return new LimitRule(Math.max(1, capacity), period);
	}

	private int pick(Map<String, Integer> cfg, String key, int fallback) {
		if (cfg == null) {
			return fallback;
		}
		Integer value = cfg.get(key);
		return (value == null || value <= 0) ? fallback : value;
	}

	private String currentSubject() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth instanceof JwtAuthenticationToken jwtAuth) {
			return jwtAuth.getToken().getSubject();
		}
		if (auth == null || !auth.isAuthenticated()) {
			return null;
		}
		String name = auth.getName();
		if (name == null || name.isBlank() || "anonymousUser".equals(name)) {
			return null;
		}
		return name;
	}

	private String clientIp(HttpServletRequest request) {
		String forwardedFor = request.getHeader("X-Forwarded-For");
		if (forwardedFor != null && !forwardedFor.isBlank()) {
			int commaIndex = forwardedFor.indexOf(',');
			return commaIndex >= 0 ? forwardedFor.substring(0, commaIndex).trim() : forwardedFor.trim();
		}
		String remoteAddr = request.getRemoteAddr();
		return remoteAddr == null ? "unknown" : remoteAddr;
	}

	private record ResolvedPolicy(String key, List<LimitRule> limits) {
	}

	private record LimitRule(int capacity, Duration period) {
	}
}
