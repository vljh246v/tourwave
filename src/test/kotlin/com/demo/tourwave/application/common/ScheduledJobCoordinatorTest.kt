package com.demo.tourwave.application.common

import com.demo.tourwave.adapter.out.persistence.operations.InMemoryWorkerJobLockRepositoryAdapter
import com.demo.tourwave.domain.common.JobExecutionStatus
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ScheduledJobCoordinatorTest {
    private val clock = Clock.fixed(Instant.parse("2026-03-18T00:00:00Z"), ZoneOffset.UTC)
    private val workerJobLockRepository = InMemoryWorkerJobLockRepositoryAdapter()
    private val jobExecutionMonitor = JobExecutionMonitor()
    private val meterRegistry = SimpleMeterRegistry()
    private val coordinator =
        ScheduledJobCoordinator(
            workerJobLockRepository = workerJobLockRepository,
            jobExecutionMonitor = jobExecutionMonitor,
            meterRegistry = meterRegistry,
            clock = clock,
            ownerId = "worker-a",
            leaseDuration = Duration.ofSeconds(120),
        )

    @BeforeEach
    fun setUp() {
        workerJobLockRepository.clear()
        jobExecutionMonitor.clear()
        meterRegistry.clear()
    }

    @Test
    fun `coordinator records success metrics and releases lock`() {
        val result =
            coordinator.run(
                jobName = "offer-expiration",
                onSkipped = { "skipped" },
            ) {
                "ok"
            }

        assertEquals("ok", result)
        assertEquals(0, workerJobLockRepository.findAll().size)
        val snapshot = requireNotNull(jobExecutionMonitor.getSnapshot("offer-expiration"))
        assertEquals(JobExecutionStatus.SUCCESS, snapshot.status)
        assertEquals(
            1.0,
            meterRegistry.get("tourwave.job.execution").tag("job", "offer-expiration").tag("status", "success").counter().count(),
        )
    }

    @Test
    fun `coordinator returns skipped result when another owner holds the lock`() {
        workerJobLockRepository.tryAcquire(
            lockName = "refund-retry",
            ownerId = "worker-b",
            lockedAtUtc = clock.instant(),
            leaseExpiresAtUtc = clock.instant().plusSeconds(60),
        )

        val result =
            coordinator.run(
                jobName = "refund-retry",
                onSkipped = { "skipped" },
            ) {
                "executed"
            }

        assertEquals("skipped", result)
        val snapshot = requireNotNull(jobExecutionMonitor.getSnapshot("refund-retry"))
        assertEquals(JobExecutionStatus.SKIPPED, snapshot.status)
        assertEquals(1.0, meterRegistry.get("tourwave.job.lock.skipped").tag("job", "refund-retry").counter().count())
    }

    @Test
    fun `coordinator records failure metrics and releases lock`() {
        assertFailsWith<IllegalStateException> {
            coordinator.run(
                jobName = "finance-reconciliation",
                onSkipped = { "skipped" },
            ) {
                error("boom")
            }
        }

        assertEquals(0, workerJobLockRepository.findAll().size)
        val snapshot = requireNotNull(jobExecutionMonitor.getSnapshot("finance-reconciliation"))
        assertEquals(JobExecutionStatus.FAILURE, snapshot.status)
        assertEquals(
            1.0,
            meterRegistry.get("tourwave.job.execution").tag("job", "finance-reconciliation").tag("status", "failure").counter().count(),
        )
    }
}
