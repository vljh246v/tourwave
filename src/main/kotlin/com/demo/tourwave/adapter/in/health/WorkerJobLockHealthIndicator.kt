package com.demo.tourwave.adapter.`in`.health

import com.demo.tourwave.application.common.port.WorkerJobLockRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration

@Component("workerJobLocks")
class WorkerJobLockHealthIndicator(
    private val workerJobLockRepository: WorkerJobLockRepository,
    private val clock: Clock,
    @Value("\${tourwave.jobs.lock.stale-after-seconds:300}")
    private val staleAfterSeconds: Long,
) : HealthIndicator {
    override fun health(): Health {
        val now = clock.instant()
        val staleThreshold = now.minus(Duration.ofSeconds(staleAfterSeconds))
        val locks = workerJobLockRepository.findAll()
        val staleLocks = locks.filter { it.leaseExpiresAtUtc.isBefore(staleThreshold) }
        val health = if (staleLocks.isEmpty()) Health.up() else Health.down()
        return health
            .withDetail("activeLockCount", locks.size)
            .withDetail("staleLockCount", staleLocks.size)
            .withDetail(
                "locks",
                locks.map { lock ->
                    mapOf(
                        "lockName" to lock.lockName,
                        "ownerId" to lock.ownerId,
                        "leaseExpiresAtUtc" to lock.leaseExpiresAtUtc.toString(),
                    )
                },
            )
            .build()
    }
}
