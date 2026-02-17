package com.pehlione.web.webhook;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "payment_webhook_events",
		uniqueConstraints = @UniqueConstraint(name = "uk_pwe_provider_event", columnNames = { "provider", "event_id" }))
public class PaymentWebhookEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 16)
	private String provider;

	@Column(name = "event_id", nullable = false, length = 128)
	private String eventId;

	@Column(name = "payload_hash", nullable = false, length = 64)
	private String payloadHash;

	@Column(name = "received_at", nullable = false, updatable = false)
	private Instant receivedAt = Instant.now();

	public Long getId() {
		return id;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public String getEventId() {
		return eventId;
	}

	public void setEventId(String eventId) {
		this.eventId = eventId;
	}

	public String getPayloadHash() {
		return payloadHash;
	}

	public void setPayloadHash(String payloadHash) {
		this.payloadHash = payloadHash;
	}

	public Instant getReceivedAt() {
		return receivedAt;
	}
}
