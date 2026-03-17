CREATE TABLE IF NOT EXISTS organizations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  slug VARCHAR(120) NOT NULL,
  name VARCHAR(255) NOT NULL,
  description TEXT NULL,
  public_description TEXT NULL,
  contact_email VARCHAR(255) NULL,
  contact_phone VARCHAR(64) NULL,
  website_url VARCHAR(512) NULL,
  business_name VARCHAR(255) NULL,
  business_registration_number VARCHAR(128) NULL,
  timezone VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_organizations_slug UNIQUE (slug)
);

CREATE TABLE IF NOT EXISTS organization_members (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  organization_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  role VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_organization_members_org_user UNIQUE (organization_id, user_id),
  INDEX idx_organization_members_org_status (organization_id, status),
  INDEX idx_organization_members_user_status (user_id, status)
);
