CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL,
  CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS occurrences (
  id BIGINT PRIMARY KEY,
  organization_id BIGINT NOT NULL,
  tour_id BIGINT NULL,
  instructor_profile_id BIGINT NULL,
  capacity INT NOT NULL,
  starts_at_utc DATETIME(6) NULL,
  timezone VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL
);

CREATE TABLE IF NOT EXISTS bookings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  occurrence_id BIGINT NOT NULL,
  organization_id BIGINT NOT NULL,
  leader_user_id BIGINT NOT NULL,
  party_size INT NOT NULL,
  status VARCHAR(32) NOT NULL,
  payment_status VARCHAR(32) NOT NULL,
  offer_expires_at_utc DATETIME(6) NULL,
  waitlist_skip_count INT NOT NULL DEFAULT 0,
  last_waitlist_action_at_utc DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  version BIGINT NULL,
  INDEX idx_bookings_occurrence (occurrence_id),
  INDEX idx_bookings_occurrence_status (occurrence_id, status),
  INDEX idx_bookings_status_offer (status, offer_expires_at_utc)
);

CREATE TABLE IF NOT EXISTS payment_records (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  booking_id BIGINT NOT NULL,
  status VARCHAR(48) NOT NULL,
  last_refund_request_id VARCHAR(128) NULL,
  last_refund_reason_code VARCHAR(128) NULL,
  last_error_code VARCHAR(128) NULL,
  created_at_utc DATETIME(6) NOT NULL,
  updated_at_utc DATETIME(6) NOT NULL,
  CONSTRAINT uk_payment_records_booking UNIQUE (booking_id)
);

CREATE TABLE IF NOT EXISTS booking_participants (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  booking_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL,
  attendance_status VARCHAR(32) NOT NULL,
  invited_at DATETIME(6) NULL,
  responded_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_booking_participants_booking_user UNIQUE (booking_id, user_id),
  INDEX idx_booking_participants_booking (booking_id),
  INDEX idx_booking_participants_status (status)
);

CREATE TABLE IF NOT EXISTS inquiries (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  organization_id BIGINT NOT NULL,
  occurrence_id BIGINT NOT NULL,
  booking_id BIGINT NOT NULL,
  created_by_user_id BIGINT NOT NULL,
  subject VARCHAR(255) NULL,
  status VARCHAR(16) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_inquiries_booking UNIQUE (booking_id),
  INDEX idx_inquiries_creator_created (created_by_user_id, created_at)
);

CREATE TABLE IF NOT EXISTS inquiry_messages (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  inquiry_id BIGINT NOT NULL,
  sender_user_id BIGINT NOT NULL,
  body TEXT NOT NULL,
  attachment_asset_ids_csv TEXT NULL,
  created_at DATETIME(6) NOT NULL,
  INDEX idx_inquiry_messages_inquiry (inquiry_id, created_at)
);

CREATE TABLE IF NOT EXISTS reviews (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  occurrence_id BIGINT NOT NULL,
  reviewer_user_id BIGINT NOT NULL,
  type VARCHAR(24) NOT NULL,
  rating INT NOT NULL,
  comment TEXT NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_reviews_occurrence_reviewer_type UNIQUE (occurrence_id, reviewer_user_id, type),
  INDEX idx_reviews_occurrence_type (occurrence_id, type)
);

CREATE TABLE IF NOT EXISTS idempotency_records (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  actor_user_id BIGINT NOT NULL,
  method VARCHAR(16) NOT NULL,
  path_template VARCHAR(255) NOT NULL,
  idempotency_key VARCHAR(255) NOT NULL,
  request_hash VARCHAR(64) NOT NULL,
  state VARCHAR(16) NOT NULL,
  response_status INT NULL,
  response_body_json LONGTEXT NULL,
  response_body_type VARCHAR(255) NULL,
  created_at_utc DATETIME(6) NOT NULL,
  completed_at_utc DATETIME(6) NULL,
  expires_at_utc DATETIME(6) NOT NULL,
  CONSTRAINT uk_idempotency_scope UNIQUE (actor_user_id, method, path_template, idempotency_key),
  INDEX idx_idempotency_expires_at (expires_at_utc)
);
