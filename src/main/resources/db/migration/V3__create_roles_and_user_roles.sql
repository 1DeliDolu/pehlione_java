CREATE TABLE IF NOT EXISTS roles (
	id BIGINT NOT NULL AUTO_INCREMENT,
	name VARCHAR(64) NOT NULL,
	PRIMARY KEY (id),
	UNIQUE KEY uk_roles_name (name)
);

CREATE TABLE IF NOT EXISTS user_roles (
	user_id BIGINT NOT NULL,
	role_id BIGINT NOT NULL,
	PRIMARY KEY (user_id, role_id),
	CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
	CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);

-- users.roles legacy kolonu mapped olmadığı için null kabul etsin.
ALTER TABLE users MODIFY COLUMN roles VARCHAR(255) NULL;

INSERT IGNORE INTO roles (name) VALUES ('ROLE_USER'), ('ROLE_ADMIN');
