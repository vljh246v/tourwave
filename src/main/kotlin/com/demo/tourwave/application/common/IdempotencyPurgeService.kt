package com.demo.tourwave.application.common

import com.demo.tourwave.application.common.port.AuditEventCommand
import com.demo.tourwave.application.common.port.AuditEventPort
import com.demo.tourwave.application.common.port.IdempotencyMaintenancePort
import java.time.Clock

data class IdempotencyPurgeJobResult(
    val purgedCount: Long
)

class IdempotencyPurgeService(
    private val idempotencyMaintenancePort: IdempotencyMaintenancePort,
    private val auditEventPort: AuditEventPort,
    private val clock: Clock
) {
    fun purgeExpired(): IdempotencyPurgeJobResult {
        val purged = idempotencyMaintenancePort.purgeExpired(clock.instant().toEpochMilli())
        auditEventPort.append(
            AuditEventCommand(
                actor = "JOB:0",
                action = "IDEMPOTENCY_PURGED",
                resourceType = "IDEMPOTENCY_RECORD",
                resourceId = 0L,
                occurredAtUtc = clock.instant(),
                reasonCode = "IDEMPOTENCY_TTL_PURGE",
                details = mapOf("purgedCount" to purged)
            )
        )
        return IdempotencyPurgeJobResult(purgedCount = purged)
    }
}
