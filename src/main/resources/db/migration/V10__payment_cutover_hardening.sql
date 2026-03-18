ALTER TABLE payment_records ADD COLUMN next_retry_at_utc TIMESTAMP;
ALTER TABLE payment_records ADD COLUMN last_remediation_action VARCHAR(64);
ALTER TABLE payment_records ADD COLUMN last_remediated_by_user_id BIGINT;
ALTER TABLE payment_records ADD COLUMN last_remediated_at_utc TIMESTAMP;

ALTER TABLE payment_provider_events ADD COLUMN signature_key_id VARCHAR(64);
ALTER TABLE payment_provider_events ADD COLUMN payload_sha256 VARCHAR(64) NOT NULL DEFAULT '';

ALTER TABLE payment_reconciliation_daily_summaries ADD COLUMN provider_captured_count INT NOT NULL DEFAULT 0;
ALTER TABLE payment_reconciliation_daily_summaries ADD COLUMN provider_refunded_count INT NOT NULL DEFAULT 0;
ALTER TABLE payment_reconciliation_daily_summaries ADD COLUMN capture_mismatch_count INT NOT NULL DEFAULT 0;
ALTER TABLE payment_reconciliation_daily_summaries ADD COLUMN refund_mismatch_count INT NOT NULL DEFAULT 0;
ALTER TABLE payment_reconciliation_daily_summaries ADD COLUMN internal_status_mismatch_count INT NOT NULL DEFAULT 0;
