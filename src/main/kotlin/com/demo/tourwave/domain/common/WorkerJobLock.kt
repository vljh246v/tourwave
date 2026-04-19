package com.demo.tourwave.domain.common

import java.time.Instant

data class WorkerJobLock(
    val lockName: String,
    val ownerId: String,
    val lockedAtUtc: Instant,
    val leaseExpiresAtUtc: Instant,
)
