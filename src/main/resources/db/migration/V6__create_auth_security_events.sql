CREATE TABLE auth_security_events (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NULL,
  event_type VARCHAR(64) NOT NULL,
  ip VARCHAR(64) NULL,
  user_agent VARCHAR(255) NULL,
  details VARCHAR(1000) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_auth_events_user (user_id),
  KEY idx_auth_events_type (event_type),
  CONSTRAINT fk_auth_events_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);
