CREATE TABLE cart_items (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cart_user_product (user_id, product_id),
  KEY idx_cart_user (user_id),
  CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE TABLE order_drafts (
  id BIGINT NOT NULL AUTO_INCREMENT,
  public_id CHAR(36) NOT NULL,
  user_id BIGINT NOT NULL,
  status VARCHAR(16) NOT NULL,
  currency CHAR(3) NOT NULL DEFAULT 'EUR',
  total_amount DECIMAL(19,2) NOT NULL DEFAULT 0.00,
  expires_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_drafts_public_id (public_id),
  KEY idx_order_drafts_user (user_id),
  KEY idx_order_drafts_status_exp (status, expires_at),
  CONSTRAINT fk_order_drafts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE order_draft_items (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_draft_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  sku VARCHAR(64) NOT NULL,
  name VARCHAR(255) NOT NULL,
  unit_price DECIMAL(19,2) NOT NULL,
  currency CHAR(3) NOT NULL,
  quantity INT NOT NULL,
  line_total DECIMAL(19,2) NOT NULL,
  reservation_id BIGINT NULL,
  reservation_public_id CHAR(36) NULL,
  reservation_expires_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_odi_draft (order_draft_id),
  KEY idx_odi_product (product_id),
  CONSTRAINT fk_odi_draft FOREIGN KEY (order_draft_id) REFERENCES order_drafts(id) ON DELETE CASCADE,
  CONSTRAINT fk_odi_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
  CONSTRAINT fk_odi_res FOREIGN KEY (reservation_id) REFERENCES inventory_reservations(id) ON DELETE SET NULL
);
