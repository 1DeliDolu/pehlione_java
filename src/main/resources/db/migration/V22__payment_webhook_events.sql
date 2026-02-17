CREATE TABLE payment_webhook_events (
	id BIGINT NOT NULL AUTO_INCREMENT,
	provider VARCHAR(16) NOT NULL,
	event_id VARCHAR(128) NOT NULL,
	payload_hash CHAR(64) NOT NULL,
	received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (id),
	UNIQUE KEY uk_pwe_provider_event (provider, event_id),
	KEY idx_pwe_received (received_at)
);
