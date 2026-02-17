CREATE TABLE user_addresses (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  label VARCHAR(64) NULL,
  full_name VARCHAR(128) NOT NULL,
  phone VARCHAR(32) NULL,
  line1 VARCHAR(255) NOT NULL,
  line2 VARCHAR(255) NULL,
  city VARCHAR(128) NOT NULL,
  state VARCHAR(128) NULL,
  postal_code VARCHAR(32) NOT NULL,
  country_code CHAR(2) NOT NULL,
  is_default BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_addr_user (user_id),
  KEY idx_addr_user_default (user_id, is_default),
  CONSTRAINT fk_addr_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE order_shipping_addresses (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  full_name VARCHAR(128) NOT NULL,
  phone VARCHAR(32) NULL,
  line1 VARCHAR(255) NOT NULL,
  line2 VARCHAR(255) NULL,
  city VARCHAR(128) NOT NULL,
  state VARCHAR(128) NULL,
  postal_code VARCHAR(32) NOT NULL,
  country_code CHAR(2) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_osa_order (order_id),
  CONSTRAINT fk_osa_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);
