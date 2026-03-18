ALTER TABLE payment_records
    ADD COLUMN next_retry_at_utc TIMESTAMP NULL AFTER last_refund_attempted_at_utc,
    ADD COLUMN last_remediation_action VARCHAR(64) NULL AFTER next_retry_at_utc,
    ADD COLUMN last_remediated_by_user_id BIGINT NULL AFTER last_remediation_action,
    ADD COLUMN last_remediated_at_utc TIMESTAMP NULL AFTER last_remediated_by_user_id;

ALTER TABLE payment_provider_events
    ADD COLUMN signature_key_id VARCHAR(64) NULL AFTER signature,
    ADD COLUMN payload_sha256 VARCHAR(64) NOT NULL DEFAULT '' AFTER signature_key_id;

UPDATE payment_provider_events
SET payload_sha256 = SHA2(payload_json, 256)
WHERE payload_sha256 = '';

ALTER TABLE payment_reconciliation_daily_summaries
    ADD COLUMN provider_captured_count INT NOT NULL DEFAULT 0 AFTER captured_count,
    ADD COLUMN provider_refunded_count INT NOT NULL DEFAULT 0 AFTER provider_captured_count,
    ADD COLUMN capture_mismatch_count INT NOT NULL DEFAULT 0 AFTER refund_review_required_count,
    ADD COLUMN refund_mismatch_count INT NOT NULL DEFAULT 0 AFTER capture_mismatch_count,
    ADD COLUMN internal_status_mismatch_count INT NOT NULL DEFAULT 0 AFTER refund_mismatch_count;
