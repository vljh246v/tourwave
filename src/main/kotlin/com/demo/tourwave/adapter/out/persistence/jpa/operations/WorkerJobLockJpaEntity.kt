package com.demo.tourwave.adapter.out.persistence.jpa.operations

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(
    name = "worker_job_locks",
    indexes = [
        Index(name = "idx_worker_job_locks_lease_expires_at_utc", columnList = "lease_expires_at_utc")
    ]
)
data class WorkerJobLockJpaEntity(
    @Id
    @Column(name = "lock_name", nullable = false, length = 100)
    val lockName: String,
    @Column(name = "owner_id", nullable = false, length = 191)
    val ownerId: String,
    @Column(name = "locked_at_utc", nullable = false)
    val lockedAtUtc: Instant,
    @Column(name = "lease_expires_at_utc", nullable = false)
    val leaseExpiresAtUtc: Instant
)
