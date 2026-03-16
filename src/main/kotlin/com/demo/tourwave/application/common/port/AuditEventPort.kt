package com.demo.tourwave.application.common.port

import java.time.Instant

data class AuditEventCommand(
    val actor: String,
    val action: String,
    val resourceType: String,
    val resourceId: Long,
    val occurredAtUtc: Instant,
    val requestId: String? = null,
    val details: Map<String, Any?> = emptyMap()
)

interface AuditEventPort {
    fun append(event: AuditEventCommand)
}
