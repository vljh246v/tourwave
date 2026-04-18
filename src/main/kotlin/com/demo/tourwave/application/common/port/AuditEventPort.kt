package com.demo.tourwave.application.common.port

import java.time.Instant

enum class AuditActorType {
    USER,
    OPERATOR,
    SYSTEM,
    JOB,
}

data class AuditEventCommand(
    val actor: String,
    val action: String,
    val resourceType: String,
    val resourceId: Long,
    val occurredAtUtc: Instant,
    val requestId: String? = null,
    val details: Map<String, Any?> = emptyMap(),
    val actorType: AuditActorType = parseActorType(actor),
    val actorId: Long? = parseActorId(actor),
    val reasonCode: String? = null,
    val beforeJson: Map<String, Any?>? = null,
    val afterJson: Map<String, Any?>? = null,
) {
    companion object {
        private fun parseActorType(actor: String): AuditActorType {
            return when (actor.substringBefore(':').uppercase()) {
                "USER" -> AuditActorType.USER
                "OPERATOR" -> AuditActorType.OPERATOR
                "JOB" -> AuditActorType.JOB
                else -> AuditActorType.SYSTEM
            }
        }

        private fun parseActorId(actor: String): Long? {
            return actor.substringAfter(':', "").toLongOrNull()
        }
    }
}

interface AuditEventPort {
    fun append(event: AuditEventCommand)
}
