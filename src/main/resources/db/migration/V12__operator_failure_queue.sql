CREATE TABLE IF NOT EXISTS operator_failure_records (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  source_type VARCHAR(64) NOT NULL,
  source_key VARCHAR(191) NOT NULL,
  status VARCHAR(32) NOT NULL,
  last_action VARCHAR(32) NOT NULL,
  note VARCHAR(500) NULL,
  last_action_by_user_id BIGINT NOT NULL,
  last_action_at_utc DATETIME(6) NOT NULL,
  retry_count INT NOT NULL DEFAULT 0,
  created_at_utc DATETIME(6) NOT NULL,
  updated_at_utc DATETIME(6) NOT NULL,
  UNIQUE KEY uk_operator_failure_records_source (source_type, source_key)
);

CREATE INDEX idx_operator_failure_records_updated_at_utc
  ON operator_failure_records (updated_at_utc);
