package com.demo.tourwave.adapter.out.persistence.jpa.operations

import com.demo.tourwave.application.common.port.WorkerJobLockRepository
import com.demo.tourwave.domain.common.WorkerJobLock
import org.springframework.context.annotation.Profile
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Repository
@Profile("mysql", "mysql-test")
class JpaWorkerJobLockRepositoryAdapter(
    private val workerJobLockJpaRepository: WorkerJobLockJpaRepository
) : WorkerJobLockRepository {
    @Transactional
    override fun tryAcquire(lockName: String, ownerId: String, lockedAtUtc: Instant, leaseExpiresAtUtc: Instant): Boolean {
        val current = workerJobLockJpaRepository.findLockedByLockName(lockName)
        if (current == null) {
            return try {
                workerJobLockJpaRepository.save(
                    WorkerJobLockJpaEntity(
                        lockName = lockName,
                        ownerId = ownerId,
                        lockedAtUtc = lockedAtUtc,
                        leaseExpiresAtUtc = leaseExpiresAtUtc
                    )
                )
                true
            } catch (_: DataIntegrityViolationException) {
                false
            }
        }
        if (current.ownerId != ownerId && current.leaseExpiresAtUtc.isAfter(lockedAtUtc)) {
            return false
        }
        workerJobLockJpaRepository.save(
            current.copy(
                ownerId = ownerId,
                lockedAtUtc = lockedAtUtc,
                leaseExpiresAtUtc = leaseExpiresAtUtc
            )
        )
        return true
    }

    @Transactional
    override fun release(lockName: String, ownerId: String) {
        val current = workerJobLockJpaRepository.findLockedByLockName(lockName) ?: return
        if (current.ownerId == ownerId) {
            workerJobLockJpaRepository.delete(current)
        }
    }

    override fun findAll(): List<WorkerJobLock> =
        workerJobLockJpaRepository.findAll()
            .sortedBy { it.lockName }
            .map { it.toDomain() }

    override fun clear() {
        workerJobLockJpaRepository.deleteAllInBatch()
    }
}

private fun WorkerJobLockJpaEntity.toDomain(): WorkerJobLock =
    WorkerJobLock(
        lockName = lockName,
        ownerId = ownerId,
        lockedAtUtc = lockedAtUtc,
        leaseExpiresAtUtc = leaseExpiresAtUtc
    )
