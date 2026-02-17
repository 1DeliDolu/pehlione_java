CREATE TABLE inventory_reservations (
  id BIGINT NOT NULL AUTO_INCREMENT,
  public_id CHAR(36) NOT NULL,
  product_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  status VARCHAR(16) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_inv_res_public_id (public_id),
  KEY idx_inv_res_product (product_id),
  KEY idx_inv_res_user (user_id),
  KEY idx_inv_res_status_exp (status, expires_at),
  CONSTRAINT fk_inv_res_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
  CONSTRAINT fk_inv_res_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE inventory_events (
  id BIGINT NOT NULL AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  reservation_id BIGINT NULL,
  actor_user_id BIGINT NULL,
  type VARCHAR(16) NOT NULL,
  quantity_delta INT NOT NULL,
  reason VARCHAR(255) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_inv_ev_product (product_id),
  KEY idx_inv_ev_reservation (reservation_id),
  KEY idx_inv_ev_type (type),
  CONSTRAINT fk_inv_ev_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
  CONSTRAINT fk_inv_ev_reservation FOREIGN KEY (reservation_id) REFERENCES inventory_reservations(id) ON DELETE SET NULL,
  CONSTRAINT fk_inv_ev_actor FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE SET NULL
);
