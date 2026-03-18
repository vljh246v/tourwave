package com.demo.tourwave.adapter.out.persistence.operations

import com.demo.tourwave.application.common.port.WorkerJobLockRepository
import com.demo.tourwave.domain.common.WorkerJobLock
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Repository
@Profile("!mysql & !mysql-test")
class InMemoryWorkerJobLockRepositoryAdapter : WorkerJobLockRepository {
    private val locks = ConcurrentHashMap<String, WorkerJobLock>()

    override fun tryAcquire(lockName: String, ownerId: String, lockedAtUtc: Instant, leaseExpiresAtUtc: Instant): Boolean {
        synchronized(locks) {
            val current = locks[lockName]
            if (current == null || !current.leaseExpiresAtUtc.isAfter(lockedAtUtc) || current.ownerId == ownerId) {
                locks[lockName] = WorkerJobLock(
                    lockName = lockName,
                    ownerId = ownerId,
                    lockedAtUtc = lockedAtUtc,
                    leaseExpiresAtUtc = leaseExpiresAtUtc
                )
                return true
            }
            return false
        }
    }

    override fun release(lockName: String, ownerId: String) {
        synchronized(locks) {
            val current = locks[lockName] ?: return
            if (current.ownerId == ownerId) {
                locks.remove(lockName)
            }
        }
    }

    override fun findAll(): List<WorkerJobLock> = locks.values.sortedBy { it.lockName }

    override fun clear() {
        locks.clear()
    }
}
