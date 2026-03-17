package com.demo.tourwave.adapter.`in`.job

import com.demo.tourwave.application.common.IdempotencyPurgeJobResult
import com.demo.tourwave.application.common.IdempotencyPurgeService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "tourwave.jobs.idempotency-purge",
    name = ["enabled"],
    havingValue = "true"
)
class IdempotencyPurgeJob(
    private val idempotencyPurgeService: IdempotencyPurgeService
) {
    @Scheduled(fixedDelayString = "\${tourwave.jobs.idempotency-purge.fixed-delay-ms:300000}")
    fun run(): IdempotencyPurgeJobResult = idempotencyPurgeService.purgeExpired()
}
