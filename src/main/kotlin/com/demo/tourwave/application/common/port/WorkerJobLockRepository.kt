package com.demo.tourwave.application.common.port

import com.demo.tourwave.domain.common.WorkerJobLock
import java.time.Instant

interface WorkerJobLockRepository {
    fun tryAcquire(lockName: String, ownerId: String, lockedAtUtc: Instant, leaseExpiresAtUtc: Instant): Boolean

    fun release(lockName: String, ownerId: String)

    fun findAll(): List<WorkerJobLock>

    fun clear()
}
