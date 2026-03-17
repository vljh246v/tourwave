ALTER TABLE users ADD COLUMN password_hash VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE users ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE users ADD COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE users ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE users ADD COLUMN email_verified_at DATETIME(6) NULL;

CREATE TABLE IF NOT EXISTS auth_refresh_tokens (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  token_hash VARCHAR(128) NOT NULL,
  expires_at_utc DATETIME(6) NOT NULL,
  issued_at_utc DATETIME(6) NOT NULL,
  revoked_at_utc DATETIME(6) NULL,
  INDEX idx_auth_refresh_tokens_user (user_id),
  INDEX idx_auth_refresh_tokens_expires (expires_at_utc),
  CONSTRAINT uk_auth_refresh_tokens_hash UNIQUE (token_hash)
);

CREATE TABLE IF NOT EXISTS user_action_tokens (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  token_hash VARCHAR(128) NOT NULL,
  purpose VARCHAR(64) NOT NULL,
  expires_at_utc DATETIME(6) NOT NULL,
  created_at_utc DATETIME(6) NOT NULL,
  consumed_at_utc DATETIME(6) NULL,
  INDEX idx_user_action_tokens_user_purpose (user_id, purpose),
  INDEX idx_user_action_tokens_expires (expires_at_utc),
  CONSTRAINT uk_user_action_tokens_hash UNIQUE (token_hash)
);
