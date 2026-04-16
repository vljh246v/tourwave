package com.demo.tourwave.adapter.`in`.job

import com.demo.tourwave.application.common.IdempotencyPurgeJobResult
import com.demo.tourwave.application.common.IdempotencyPurgeService
import com.demo.tourwave.application.common.ScheduledJobCoordinator
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "tourwave.jobs.idempotency-purge",
    name = ["enabled"],
    havingValue = "true",
)
class IdempotencyPurgeJob(
    private val idempotencyPurgeService: IdempotencyPurgeService,
    private val scheduledJobCoordinator: ScheduledJobCoordinator,
) {
    @Scheduled(fixedDelayString = "\${tourwave.jobs.idempotency-purge.fixed-delay-ms:300000}")
    fun run(): IdempotencyPurgeJobResult =
        scheduledJobCoordinator.run(
            jobName = "idempotency-purge",
            onSkipped = { IdempotencyPurgeJobResult(purgedCount = 0) },
        ) {
            idempotencyPurgeService.purgeExpired()
        }
}
