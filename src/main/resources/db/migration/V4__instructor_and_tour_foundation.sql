CREATE TABLE IF NOT EXISTS instructor_registrations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  organization_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  headline VARCHAR(255) NULL,
  bio TEXT NULL,
  languages_json TEXT NOT NULL,
  specialties_json TEXT NOT NULL,
  status VARCHAR(32) NOT NULL,
  rejection_reason TEXT NULL,
  reviewed_by_user_id BIGINT NULL,
  reviewed_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_instructor_registrations_org_user UNIQUE (organization_id, user_id),
  INDEX idx_instructor_registrations_org_status (organization_id, status),
  INDEX idx_instructor_registrations_user_status (user_id, status)
);

CREATE TABLE IF NOT EXISTS instructor_profiles (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  organization_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  headline VARCHAR(255) NULL,
  bio TEXT NULL,
  languages_json TEXT NOT NULL,
  specialties_json TEXT NOT NULL,
  certifications_json TEXT NOT NULL,
  years_of_experience INT NULL,
  internal_note TEXT NULL,
  status VARCHAR(32) NOT NULL,
  approved_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_instructor_profiles_org_user UNIQUE (organization_id, user_id),
  INDEX idx_instructor_profiles_org_status (organization_id, status),
  INDEX idx_instructor_profiles_user_status (user_id, status)
);

CREATE TABLE IF NOT EXISTS tours (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  organization_id BIGINT NOT NULL,
  title VARCHAR(255) NOT NULL,
  summary TEXT NULL,
  status VARCHAR(32) NOT NULL,
  description TEXT NULL,
  highlights_json TEXT NOT NULL,
  inclusions_json TEXT NOT NULL,
  exclusions_json TEXT NOT NULL,
  preparations_json TEXT NOT NULL,
  policies_json TEXT NOT NULL,
  published_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  INDEX idx_tours_org_status (organization_id, status)
);
