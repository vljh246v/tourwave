package com.demo.tourwave.adapter.`in`.health

import com.demo.tourwave.application.common.JobExecutionMonitor
import com.demo.tourwave.domain.common.JobExecutionStatus
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component

@Component("workerJobs")
class JobExecutionHealthIndicator(
    private val jobExecutionMonitor: JobExecutionMonitor,
) : HealthIndicator {
    override fun health(): Health {
        val snapshots = jobExecutionMonitor.getSnapshots()
        if (snapshots.isEmpty()) {
            return Health.unknown()
                .withDetail("registeredJobs", 0)
                .withDetail("message", "No scheduled job execution has been recorded yet")
                .build()
        }
        val failures = snapshots.filter { it.status == JobExecutionStatus.FAILURE }
        val health = if (failures.isEmpty()) Health.up() else Health.down()
        return health
            .withDetail("registeredJobs", snapshots.size)
            .withDetail("failedJobs", failures.map { it.jobName })
            .withDetail(
                "jobs",
                snapshots.map { snapshot ->
                    mapOf(
                        "jobName" to snapshot.jobName,
                        "status" to snapshot.status.name,
                        "lastStartedAtUtc" to snapshot.lastStartedAtUtc?.toString(),
                        "lastFinishedAtUtc" to snapshot.lastFinishedAtUtc?.toString(),
                        "runCount" to snapshot.runCount,
                        "successCount" to snapshot.successCount,
                        "failureCount" to snapshot.failureCount,
                        "skippedCount" to snapshot.skippedCount,
                    )
                },
            )
            .build()
    }
}
