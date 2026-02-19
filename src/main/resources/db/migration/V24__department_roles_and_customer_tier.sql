-- V24: Department roles (HR, IT, Process, Marketing, Finance, Support)
--      and customer tier system (Bronze, Silver, Gold, Platinum)

-- ── 1. Department roles ──────────────────────────────────────────────────────
INSERT IGNORE INTO roles (name) VALUES ('ROLE_DEPT_HR');
INSERT IGNORE INTO roles (name) VALUES ('ROLE_DEPT_IT');
INSERT IGNORE INTO roles (name) VALUES ('ROLE_DEPT_PROCESS');
INSERT IGNORE INTO roles (name) VALUES ('ROLE_DEPT_MARKETING');
INSERT IGNORE INTO roles (name) VALUES ('ROLE_DEPT_FINANCE');
INSERT IGNORE INTO roles (name) VALUES ('ROLE_DEPT_SUPPORT');

-- ── 2. Customer tier roles ───────────────────────────────────────────────────
INSERT IGNORE INTO roles (name) VALUES ('ROLE_TIER_BRONZE');
INSERT IGNORE INTO roles (name) VALUES ('ROLE_TIER_SILVER');
INSERT IGNORE INTO roles (name) VALUES ('ROLE_TIER_GOLD');
INSERT IGNORE INTO roles (name) VALUES ('ROLE_TIER_PLATINUM');

-- ── 3. customer_tier column on users ────────────────────────────────────────
ALTER TABLE users ADD COLUMN customer_tier VARCHAR(16) NULL;

-- ── 4. department column on users ────────────────────────────────────────────
ALTER TABLE users ADD COLUMN department VARCHAR(64) NULL;
