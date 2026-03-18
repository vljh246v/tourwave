package com.demo.tourwave.domain.common

import java.time.Instant

enum class JobExecutionStatus {
    RUNNING,
    SUCCESS,
    FAILURE,
    SKIPPED
}

data class JobExecutionSnapshot(
    val jobName: String,
    val status: JobExecutionStatus,
    val lastStartedAtUtc: Instant? = null,
    val lastFinishedAtUtc: Instant? = null,
    val lastDurationMs: Long? = null,
    val lastErrorMessage: String? = null,
    val runCount: Long = 0,
    val successCount: Long = 0,
    val failureCount: Long = 0,
    val skippedCount: Long = 0
)
