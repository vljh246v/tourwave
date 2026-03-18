package com.demo.tourwave.application.common

import com.demo.tourwave.application.common.port.WorkerJobLockRepository
import io.micrometer.core.instrument.MeterRegistry
import java.time.Clock
import java.time.Duration

class ScheduledJobCoordinator(
    private val workerJobLockRepository: WorkerJobLockRepository,
    private val jobExecutionMonitor: JobExecutionMonitor,
    private val meterRegistry: MeterRegistry,
    private val clock: Clock,
    private val ownerId: String,
    private val leaseDuration: Duration
) {
    fun <T> run(jobName: String, onSkipped: () -> T, action: () -> T): T {
        val startedAtUtc = clock.instant()
        val acquired = workerJobLockRepository.tryAcquire(
            lockName = jobName,
            ownerId = ownerId,
            lockedAtUtc = startedAtUtc,
            leaseExpiresAtUtc = startedAtUtc.plus(leaseDuration)
        )
        if (!acquired) {
            jobExecutionMonitor.recordSkipped(jobName, startedAtUtc)
            meterRegistry.counter("tourwave.job.lock.skipped", "job", jobName).increment()
            return onSkipped()
        }

        jobExecutionMonitor.recordStarted(jobName, startedAtUtc)
        val startedAtNanos = System.nanoTime()
        try {
            val result = action()
            val durationMs = Duration.ofNanos(System.nanoTime() - startedAtNanos).toMillis()
            val finishedAtUtc = clock.instant()
            jobExecutionMonitor.recordSuccess(jobName, finishedAtUtc, durationMs)
            meterRegistry.counter("tourwave.job.execution", "job", jobName, "status", "success").increment()
            meterRegistry.timer("tourwave.job.execution.duration", "job", jobName, "status", "success")
                .record(Duration.ofMillis(durationMs))
            return result
        } catch (ex: RuntimeException) {
            val durationMs = Duration.ofNanos(System.nanoTime() - startedAtNanos).toMillis()
            val finishedAtUtc = clock.instant()
            jobExecutionMonitor.recordFailure(jobName, finishedAtUtc, durationMs, ex.message)
            meterRegistry.counter("tourwave.job.execution", "job", jobName, "status", "failure").increment()
            meterRegistry.timer("tourwave.job.execution.duration", "job", jobName, "status", "failure")
                .record(Duration.ofMillis(durationMs))
            throw ex
        } finally {
            workerJobLockRepository.release(jobName, ownerId)
        }
    }
}
