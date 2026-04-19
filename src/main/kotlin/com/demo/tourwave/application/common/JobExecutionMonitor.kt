package com.demo.tourwave.application.common

import com.demo.tourwave.domain.common.JobExecutionSnapshot
import com.demo.tourwave.domain.common.JobExecutionStatus
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class JobExecutionMonitor {
    private val snapshots = ConcurrentHashMap<String, JobExecutionSnapshot>()

    fun recordStarted(
        jobName: String,
        startedAtUtc: Instant,
    ) {
        snapshots.compute(jobName) { _, current ->
            val base = current ?: JobExecutionSnapshot(jobName = jobName, status = JobExecutionStatus.RUNNING)
            base.copy(
                status = JobExecutionStatus.RUNNING,
                lastStartedAtUtc = startedAtUtc,
                runCount = base.runCount + 1,
                lastErrorMessage = null,
            )
        }
    }

    fun recordSuccess(
        jobName: String,
        finishedAtUtc: Instant,
        durationMs: Long,
    ) {
        snapshots.compute(jobName) { _, current ->
            val base = requireNotNull(current) { "Job must be started before success is recorded" }
            base.copy(
                status = JobExecutionStatus.SUCCESS,
                lastFinishedAtUtc = finishedAtUtc,
                lastDurationMs = durationMs,
                successCount = base.successCount + 1,
                lastErrorMessage = null,
            )
        }
    }

    fun recordFailure(
        jobName: String,
        finishedAtUtc: Instant,
        durationMs: Long,
        errorMessage: String?,
    ) {
        snapshots.compute(jobName) { _, current ->
            val base = requireNotNull(current) { "Job must be started before failure is recorded" }
            base.copy(
                status = JobExecutionStatus.FAILURE,
                lastFinishedAtUtc = finishedAtUtc,
                lastDurationMs = durationMs,
                failureCount = base.failureCount + 1,
                lastErrorMessage = errorMessage,
            )
        }
    }

    fun recordSkipped(
        jobName: String,
        skippedAtUtc: Instant,
    ) {
        snapshots.compute(jobName) { _, current ->
            val base = current ?: JobExecutionSnapshot(jobName = jobName, status = JobExecutionStatus.SKIPPED)
            base.copy(
                status = JobExecutionStatus.SKIPPED,
                lastFinishedAtUtc = skippedAtUtc,
                skippedCount = base.skippedCount + 1,
            )
        }
    }

    fun getSnapshot(jobName: String): JobExecutionSnapshot? = snapshots[jobName]

    fun getSnapshots(): List<JobExecutionSnapshot> = snapshots.values.sortedBy { it.jobName }

    fun clear() {
        snapshots.clear()
    }
}
