CREATE TABLE IF NOT EXISTS worker_job_locks (
    lock_name VARCHAR(100) NOT NULL,
    owner_id VARCHAR(191) NOT NULL,
    locked_at_utc TIMESTAMP(6) NOT NULL,
    lease_expires_at_utc TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (lock_name)
);

CREATE INDEX idx_worker_job_locks_lease_expires_at_utc
    ON worker_job_locks (lease_expires_at_utc);
