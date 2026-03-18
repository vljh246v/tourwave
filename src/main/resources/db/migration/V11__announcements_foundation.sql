CREATE TABLE announcements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    organization_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    visibility VARCHAR(32) NOT NULL,
    publish_starts_at_utc TIMESTAMP NULL,
    publish_ends_at_utc TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_announcements_org_created ON announcements (organization_id, created_at);
CREATE INDEX idx_announcements_visibility_window ON announcements (visibility, publish_starts_at_utc, publish_ends_at_utc);
