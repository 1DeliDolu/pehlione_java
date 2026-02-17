package com.pehlione.web.security.ratelimit;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ratelimit")
public class RateLimitProperties {

	private boolean enabled = true;
	private int bucketTtlMinutes = 30;
	private Map<String, Integer> policies = new HashMap<>();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getBucketTtlMinutes() {
		return bucketTtlMinutes;
	}

	public void setBucketTtlMinutes(int bucketTtlMinutes) {
		this.bucketTtlMinutes = bucketTtlMinutes;
	}

	public Map<String, Integer> getPolicies() {
		return policies;
	}

	public void setPolicies(Map<String, Integer> policies) {
		this.policies = (policies == null) ? new HashMap<>() : policies;
	}
}
