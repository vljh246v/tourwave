# MySQL Schema Notes (FK-minimized)

## Philosophy
- Avoid strict FK constraints to reduce operational friction.
- Enforce integrity in application logic + background reconcile jobs.
- Use **unique keys and indexes** to prevent duplication and keep queries fast.
- Denormalize for authorization where it reduces joins:
  - `bookings.organization_id` is stored (derived from occurrence->tour->org).

## Key tables (high level)
- users
- instructor_profiles
- organizations
- organization_members (roles set)
- tours (template)
- tour_occurrences
- tour_instructors (template-level assignment)
- bookings (denormalized org_id)
- booking_participants
- inquiries, inquiry_messages
- tour_reviews, instructor_reviews + rating summaries
- assets + attachments mapping tables
- notifications
- policies (cancel/refund)
- admin_notes (booking/inquiry notes)
- idempotency_keys (mutation dedup)
- payment_transactions (capture/refund attempts)
- audit_events (append-only domain audit)
- webhooks (optional)
- reports (derived; often views/materialized)

## Indexing & Uniqueness (recommended)
- organization_members: unique(org_id, user_id)
- tour_instructors: unique(tour_id, instructor_profile_id)
- bookings: index(org_id), index(occurrence_id), index(leader_user_id), index(status)
- booking_participants: unique(booking_id, user_id)
- inquiries: unique(booking_id) (MVP: one inquiry per booking)
- review tables: unique(booking_id, author_user_id, [instructor_profile_id])
- assets: index(owner_user_id), index(created_at)
- bookings: index(status, occurrence_id)
- idempotency_keys: unique(actor_user_id, method, path_template, idempotency_key)
- payment_transactions: unique(provider, provider_event_id) for callback dedupe
- audit_events: index(resource_type, resource_id, occurred_at)

## Reconcile jobs (because FK is minimized)
- Orphan cleanup:
  - inquiry_messages referencing missing inquiries
  - tour_assets referencing deleted assets
  - booking_participants referencing missing bookings/users
- Denormalized org_id correctness:
  - ensure bookings.org_id matches occurrence->tour->org
- Idempotency TTL purge:
  - remove expired idempotency records beyond retention window
- Payment retry queue:
  - retry `REFUND_PENDING` / failed external payment actions


-- =========================
-- Core
-- =========================

CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  email VARCHAR(255) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(100) NOT NULL,
  status ENUM('ACTIVE','SUSPENDED','DELETED') NOT NULL DEFAULT 'ACTIVE',
  email_verified_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  UNIQUE KEY uk_users_email (email),
  KEY idx_users_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE instructor_profiles (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  bio TEXT NULL,
  certifications TEXT NULL,
  languages_json JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  UNIQUE KEY uk_instructor_profiles_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- Organizations / Membership
-- =========================

CREATE TABLE organizations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(200) NOT NULL,
  description TEXT NULL,
  owner_user_id BIGINT NOT NULL,
  status ENUM('ACTIVE','SUSPENDED','DELETED') NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  KEY idx_org_owner (owner_user_id),
  KEY idx_org_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- roles set: ORG_MEMBER/ORG_ADMIN/ORG_OWNER
CREATE TABLE organization_members (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  organization_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  roles_json JSON NOT NULL,
  status ENUM('ACTIVE','REMOVED') NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_org_members (organization_id, user_id),
  KEY idx_org_members_org (organization_id),
  KEY idx_org_members_user (user_id),
  KEY idx_org_members_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE instructor_registrations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  organization_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  message TEXT NULL,
  status ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
  decided_by_user_id BIGINT NULL,
  decided_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_instructor_reg_org (organization_id),
  KEY idx_instructor_reg_user (user_id),
  KEY idx_instructor_reg_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- Taxonomy
-- =========================

CREATE TABLE activity_categories (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  UNIQUE KEY uk_activity_categories_name (name),
  KEY idx_activity_categories_active (is_active, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE activity_formats (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  UNIQUE KEY uk_activity_formats_name (name),
  KEY idx_activity_formats_active (is_active, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- Tours (template)
-- =========================

CREATE TABLE tours (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  organization_id BIGINT NOT NULL,
  status ENUM('DRAFT','PUBLISHED','ARCHIVED') NOT NULL DEFAULT 'DRAFT',
  title VARCHAR(200) NOT NULL,
  subtitle VARCHAR(200) NULL,
  category_id BIGINT NOT NULL,
  format_id BIGINT NOT NULL,
  location_text VARCHAR(200) NOT NULL,
  cover_asset_id BIGINT NULL,
  created_by_user_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  KEY idx_tours_org (organization_id),
  KEY idx_tours_status (status),
  KEY idx_tours_category (category_id),
  KEY idx_tours_format (format_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Editor content (TipTap/Slate/etc)
CREATE TABLE tour_contents (
  tour_id BIGINT PRIMARY KEY,
  content_json JSON NOT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Template-level instructors
CREATE TABLE tour_instructors (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tour_id BIGINT NOT NULL,
  instructor_profile_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tour_instructors (tour_id, instructor_profile_id),
  KEY idx_tour_instructors_tour (tour_id),
  KEY idx_tour_instructors_instructor (instructor_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- Occurrences (actual schedule)
-- =========================

CREATE TABLE tour_occurrences (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tour_id BIGINT NOT NULL,
  organization_id BIGINT NOT NULL, -- denormalized from tour.organization_id for faster authz
  status ENUM('SCHEDULED','CANCELED','FINISHED') NOT NULL DEFAULT 'SCHEDULED',
  timezone VARCHAR(64) NOT NULL, -- IANA tz
  start_at_utc DATETIME NOT NULL,
  end_at_utc DATETIME NOT NULL,
  capacity INT NOT NULL,
  price_amount INT NOT NULL,
  price_currency CHAR(3) NOT NULL DEFAULT 'KRW',
  created_by_user_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_occ_tour (tour_id),
  KEY idx_occ_org (organization_id),
  KEY idx_occ_time (start_at_utc, end_at_utc),
  KEY idx_occ_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- Bookings / Participants / Waitlist
-- =========================

CREATE TABLE bookings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,

  organization_id BIGINT NOT NULL,
  occurrence_id BIGINT NOT NULL,
  leader_user_id BIGINT NOT NULL,

  party_size INT NOT NULL,

  status ENUM(
    'REQUESTED',
    'WAITLISTED',
    'OFFERED',
    'CONFIRMED',
    'REJECTED',
    'CANCELED',
    'EXPIRED',
    'COMPLETED'
  ) NOT NULL,

  payment_status ENUM(
    'AUTHORIZED',
    'PAID',
    'REFUND_PENDING',
    'REFUNDED'
  ) NOT NULL DEFAULT 'AUTHORIZED',

  approved_by_user_id BIGINT NULL,
  approved_at DATETIME NULL,

  offer_expires_at_utc DATETIME NULL,

  amount_paid INT NULL,
  currency CHAR(3) NULL,

  note_to_operator TEXT NULL,

  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  KEY idx_bookings_occ (occurrence_id),
  KEY idx_bookings_org (organization_id),
  KEY idx_bookings_leader (leader_user_id),
  KEY idx_bookings_status (status)
);

CREATE TABLE booking_participants (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,

  booking_id BIGINT NOT NULL,
  user_id BIGINT NULL,
  invited_email VARCHAR(255) NULL,

  status ENUM(
    'INVITED',
    'ACCEPTED',
    'DECLINED',
    'EXPIRED'
  ) NOT NULL DEFAULT 'INVITED',

  attendance_status ENUM(
    'UNKNOWN',
    'ATTENDED',
    'NO_SHOW'
  ) NOT NULL DEFAULT 'UNKNOWN',

  expires_at_utc DATETIME NULL,

  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  UNIQUE KEY uk_booking_participants_booking_user (booking_id, user_id),
  KEY idx_booking_participants_booking (booking_id),
  KEY idx_booking_participants_email (invited_email),
  KEY idx_booking_participants_status (status)
);

CREATE TABLE waitlist_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,

  occurrence_id BIGINT NOT NULL,
  booking_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,

  party_size INT NOT NULL,

  status ENUM(
    'WAITING',
    'OFFERED',
    'DECLINED',
    'EXPIRED'
  ) NOT NULL DEFAULT 'WAITING',

  offer_expires_at_utc DATETIME NULL,

  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  KEY idx_waitlist_occ (occurrence_id),
  KEY idx_waitlist_status (status),
  KEY idx_waitlist_created (created_at)
);

-- =========================
-- Inquiries (tour 상담 티켓)
-- =========================

CREATE TABLE inquiries (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  organization_id BIGINT NOT NULL,
  occurrence_id BIGINT NOT NULL,
  booking_id BIGINT NOT NULL,
  created_by_user_id BIGINT NOT NULL,
  subject VARCHAR(200) NULL,
  status ENUM('OPEN','CLOSED') NOT NULL DEFAULT 'OPEN',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_inquiries_booking (booking_id),
  KEY idx_inquiries_org (organization_id),
  KEY idx_inquiries_occ (occurrence_id),
  KEY idx_inquiries_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE inquiry_messages (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  inquiry_id BIGINT NOT NULL,
  sender_user_id BIGINT NOT NULL,
  body TEXT NOT NULL,
  attachment_asset_ids_json JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_inquiry_messages_inquiry (inquiry_id),
  KEY idx_inquiry_messages_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- Reviews (content private; summary public)
-- =========================

CREATE TABLE reviews (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  type ENUM('TOUR','INSTRUCTOR') NOT NULL,
  organization_id BIGINT NOT NULL,
  occurrence_id BIGINT NOT NULL,
  tour_id BIGINT NULL,
  instructor_profile_id BIGINT NULL,
  instructor_profile_id_norm BIGINT AS (IFNULL(instructor_profile_id, 0)) STORED,
  author_user_id BIGINT NOT NULL,
  rating TINYINT NOT NULL,
  body TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_reviews_unique (type, occurrence_id, author_user_id, instructor_profile_id_norm),
  KEY idx_reviews_org (organization_id),
  KEY idx_reviews_occ (occurrence_id),
  KEY idx_reviews_instructor (instructor_profile_id),
  KEY idx_reviews_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Public summary cache (optional but recommended)
CREATE TABLE rating_summaries (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  target_type ENUM('TOUR','INSTRUCTOR') NOT NULL,
  target_id BIGINT NOT NULL,
  avg_rating DECIMAL(3,2) NOT NULL DEFAULT 0.00,
  review_count INT NOT NULL DEFAULT 0,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_rating_summaries (target_type, target_id),
  KEY idx_rating_summaries_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- Assets
-- =========================

CREATE TABLE assets (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  owner_user_id BIGINT NOT NULL,
  status ENUM('PENDING','READY','FAILED','DELETED') NOT NULL DEFAULT 'PENDING',
  mime_type VARCHAR(100) NOT NULL,
  size_bytes BIGINT NOT NULL,
  storage_key VARCHAR(512) NULL,
  url VARCHAR(1024) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  KEY idx_assets_owner (owner_user_id),
  KEY idx_assets_status (status),
  KEY idx_assets_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tour_assets (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tour_id BIGINT NOT NULL,
  asset_id BIGINT NOT NULL,
  type ENUM('COVER','GALLERY') NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tour_assets (tour_id, asset_id),
  KEY idx_tour_assets_tour (tour_id),
  KEY idx_tour_assets_type (type),
  KEY idx_tour_assets_sort (tour_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- Notifications
-- =========================

CREATE TABLE notifications (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  type VARCHAR(50) NOT NULL,
  title VARCHAR(200) NOT NULL,
  body VARCHAR(2000) NOT NULL,
  deep_link VARCHAR(512) NULL,
  is_read TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_notifications_user (user_id, created_at),
  KEY idx_notifications_unread (user_id, is_read, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- Policies (Cancellation)
-- =========================

CREATE TABLE cancellation_policies (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  occurrence_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_cancel_policies_occ (occurrence_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cancellation_policy_rules (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  policy_id BIGINT NOT NULL,
  rule_type ENUM('FULL_REFUND','NO_REFUND') NOT NULL,
  cutoff_hours INT NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_cancel_rules_policy (policy_id),
  KEY idx_cancel_rules_sort (policy_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- Admin Notes (internal)
-- =========================

CREATE TABLE admin_notes (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  organization_id BIGINT NOT NULL,
  target_type ENUM('OCCURRENCE','BOOKING','INQUIRY') NOT NULL,
  target_id BIGINT NOT NULL,
  author_user_id BIGINT NOT NULL,
  body TEXT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_admin_notes_org (organization_id),
  KEY idx_admin_notes_target (target_type, target_id),
  KEY idx_admin_notes_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- Idempotency Keys
-- =========================

CREATE TABLE idempotency_keys (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  actor_user_id BIGINT NOT NULL,
  method VARCHAR(10) NOT NULL,
  path_template VARCHAR(200) NOT NULL,
  idempotency_key VARCHAR(128) NOT NULL,
  request_hash CHAR(64) NOT NULL,
  response_status INT NULL,
  response_body_json JSON NULL,
  state ENUM('IN_PROGRESS','COMPLETED','FAILED') NOT NULL DEFAULT 'IN_PROGRESS',
  expires_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_idem_actor_method_path_key (actor_user_id, method, path_template, idempotency_key),
  KEY idx_idem_expires (expires_at),
  KEY idx_idem_state (state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- Payment Transactions (stub + external-ready)
-- =========================

CREATE TABLE payment_transactions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  booking_id BIGINT NOT NULL,
  organization_id BIGINT NOT NULL,
  tx_type ENUM('AUTHORIZE','CAPTURE','REFUND') NOT NULL,
  amount INT NOT NULL,
  currency CHAR(3) NOT NULL,
  status ENUM('PENDING','SUCCEEDED','FAILED') NOT NULL DEFAULT 'PENDING',
  provider VARCHAR(50) NULL,
  provider_tx_id VARCHAR(100) NULL,
  provider_event_id VARCHAR(100) NULL,
  failure_code VARCHAR(100) NULL,
  failure_message VARCHAR(500) NULL,
  request_payload_json JSON NULL,
  response_payload_json JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_payment_tx_booking (booking_id),
  KEY idx_payment_tx_org (organization_id),
  KEY idx_payment_tx_status (status),
  UNIQUE KEY uk_payment_provider_event (provider, provider_event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- Audit Events (append-only)
-- =========================

CREATE TABLE audit_events (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  occurred_at_utc DATETIME NOT NULL,
  actor_type ENUM('USER','OPERATOR','SYSTEM','JOB') NOT NULL,
  actor_user_id BIGINT NULL,
  action VARCHAR(100) NOT NULL,
  resource_type VARCHAR(50) NOT NULL,
  resource_id BIGINT NOT NULL,
  organization_id BIGINT NULL,
  reason_code VARCHAR(100) NULL,
  request_id VARCHAR(100) NULL,
  before_json JSON NULL,
  after_json JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_audit_resource_time (resource_type, resource_id, occurred_at_utc),
  KEY idx_audit_org_time (organization_id, occurred_at_utc),
  KEY idx_audit_actor_time (actor_user_id, occurred_at_utc)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- Announcements
-- =========================

CREATE TABLE announcements (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  organization_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  body TEXT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_announcements_org (organization_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- Favorites
-- =========================

CREATE TABLE favorites (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  tour_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_favorites_user_tour (user_id, tour_id),
  KEY idx_favorites_user (user_id, created_at),
  KEY idx_favorites_tour (tour_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


## Seat Allocation Rules (Occurrence Capacity Model)

This section defines how seat allocation works for tour occurrences.

Seat allocation is calculated dynamically based on booking status rather than relying on a stored counter.

### Capacity definition

Each `tour_occurrence` defines:

- `capacity`: total number of seats available for the occurrence.

Seats are consumed only by specific booking states.

### Seat allocation formula

Seat availability is computed as:

availableSeats = capacity - confirmedSeats - offeredSeats

Where:

- confirmedSeats = sum(party_size) for bookings with status = CONFIRMED
- offeredSeats = sum(party_size) for bookings with status = OFFERED and offer not expired

Example SQL concept:

```sql
confirmed_seats =
  SUM(party_size WHERE status = 'CONFIRMED')

offered_seats =
  SUM(party_size WHERE status = 'OFFERED'
      AND offer_expires_at_utc > NOW())

available_seats =
  capacity - confirmed_seats - offered_seats
```

Seats become available.

### Manual operator override

Operators may intervene manually.

Operator actions include:

- forcing an OFFER to a specific booking
- skipping a booking
- extending offer expiration

These operations may bypass automatic FIFO behavior.

### Skip behavior

If a booking is skipped by the operator:

- booking remains WAITLISTED
- next booking in queue may be offered

Operator action should be recorded in `admin_notes` and `audit_events`.

### Multiple promotions

If multiple bookings fit available seats, multiple bookings may be promoted simultaneously.

Example:

capacity = 10  
confirmed = 6  
available = 4

Waitlist:

| booking | party |
|--------|------|
| A | 2 |
| B | 2 |
| C | 3 |

Promotion:

A → OFFERED  
B → OFFERED  
C remains WAITLISTED

### Race condition protection

Promotion must occur within a transaction locking the occurrence.

Example pattern:

```sql
SELECT id, capacity
FROM tour_occurrences
WHERE id = :occurrenceId
FOR UPDATE;
```
