CREATE TABLE auth_sessions (
  id BIGINT NOT NULL AUTO_INCREMENT,
  public_id CHAR(36) NOT NULL,
  user_id BIGINT NOT NULL,
  ip VARCHAR(64) NULL,
  user_agent VARCHAR(255) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_seen_at TIMESTAMP NULL,
  revoked BOOLEAN NOT NULL DEFAULT FALSE,
  revoked_at TIMESTAMP NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_auth_sessions_public_id (public_id),
  KEY idx_auth_sessions_user (user_id),
  CONSTRAINT fk_auth_sessions_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

ALTER TABLE refresh_tokens
  ADD COLUMN session_id BIGINT NULL;

ALTER TABLE refresh_tokens
  ADD CONSTRAINT fk_refresh_tokens_session
    FOREIGN KEY (session_id) REFERENCES auth_sessions(id) ON DELETE SET NULL;

CREATE INDEX idx_refresh_tokens_session ON refresh_tokens(session_id);
