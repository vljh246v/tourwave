ALTER TABLE payment_records ADD COLUMN provider_name VARCHAR(64) NULL;
ALTER TABLE payment_records ADD COLUMN provider_payment_key VARCHAR(128) NULL;
ALTER TABLE payment_records ADD COLUMN provider_authorization_id VARCHAR(128) NULL;
ALTER TABLE payment_records ADD COLUMN provider_capture_id VARCHAR(128) NULL;
ALTER TABLE payment_records ADD COLUMN last_provider_reference VARCHAR(128) NULL;
ALTER TABLE payment_records ADD COLUMN refund_retry_count INT NOT NULL DEFAULT 0;
ALTER TABLE payment_records ADD COLUMN last_refund_attempted_at_utc DATETIME(6) NULL;
ALTER TABLE payment_records ADD COLUMN last_webhook_event_id VARCHAR(128) NULL;

CREATE TABLE IF NOT EXISTS payment_provider_events (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  provider_name VARCHAR(64) NOT NULL,
  provider_event_id VARCHAR(128) NOT NULL,
  event_type VARCHAR(64) NOT NULL,
  booking_id BIGINT NULL,
  payload_json LONGTEXT NOT NULL,
  signature VARCHAR(255) NULL,
  status VARCHAR(64) NOT NULL,
  note VARCHAR(255) NULL,
  received_at_utc DATETIME(6) NOT NULL,
  processed_at_utc DATETIME(6) NULL,
  CONSTRAINT uk_payment_provider_events_provider_event UNIQUE (provider_event_id),
  INDEX idx_payment_provider_events_booking (booking_id),
  INDEX idx_payment_provider_events_received (received_at_utc)
);

CREATE TABLE IF NOT EXISTS payment_reconciliation_daily_summaries (
  summary_date DATE PRIMARY KEY,
  booking_created_count INT NOT NULL,
  authorized_count INT NOT NULL,
  captured_count INT NOT NULL,
  refund_pending_count INT NOT NULL,
  refunded_count INT NOT NULL,
  no_refund_count INT NOT NULL,
  refund_failed_retryable_count INT NOT NULL,
  refund_review_required_count INT NOT NULL,
  refreshed_at_utc DATETIME(6) NOT NULL
);
