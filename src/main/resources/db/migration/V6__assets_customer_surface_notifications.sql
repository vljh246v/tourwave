CREATE TABLE IF NOT EXISTS assets (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  owner_user_id BIGINT NOT NULL,
  organization_id BIGINT NULL,
  status VARCHAR(32) NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  content_type VARCHAR(255) NOT NULL,
  storage_key VARCHAR(255) NOT NULL,
  upload_url TEXT NOT NULL,
  public_url TEXT NULL,
  size_bytes BIGINT NULL,
  checksum_sha256 VARCHAR(128) NULL,
  created_at TIMESTAMP NOT NULL,
  completed_at TIMESTAMP NULL,
  INDEX idx_assets_owner_created (owner_user_id, created_at),
  INDEX idx_assets_org_created (organization_id, created_at)
);

CREATE TABLE IF NOT EXISTS favorites (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  tour_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  UNIQUE KEY uk_favorites_user_tour (user_id, tour_id),
  INDEX idx_favorites_user_created (user_id, created_at)
);

CREATE TABLE IF NOT EXISTS notifications (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  type VARCHAR(64) NOT NULL,
  title VARCHAR(255) NOT NULL,
  body TEXT NOT NULL,
  resource_type VARCHAR(64) NOT NULL,
  resource_id BIGINT NOT NULL,
  read_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL,
  INDEX idx_notifications_user_created (user_id, created_at),
  INDEX idx_notifications_user_read (user_id, read_at)
);

ALTER TABLE organizations ADD COLUMN attachment_asset_ids_json TEXT NOT NULL DEFAULT ('[]');
ALTER TABLE tours ADD COLUMN attachment_asset_ids_json TEXT NOT NULL DEFAULT ('[]');
