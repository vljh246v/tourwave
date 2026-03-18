package com.demo.tourwave.adapter.out.persistence.idempotency

import com.demo.tourwave.application.common.port.IdempotencyDecision
import com.demo.tourwave.application.common.port.IdempotencyMaintenancePort
import com.demo.tourwave.application.common.port.IdempotencyStore
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

enum class IdempotencyState {
    IN_PROGRESS,
    COMPLETED,
}

data class IdempotencyRecord(
    val requestHash: String,
    var state: IdempotencyState,
    var responseStatus: Int? = null,
    var responseBody: Any? = null,
)

data class IdempotencyScope(
    val actorUserId: Long,
    val method: String,
    val pathTemplate: String,
    val idempotencyKey: String,
)

@Component
@Profile("!mysql & !mysql-test")
class InMemoryIdempotencyStoreAdapter :
    IdempotencyStore,
    IdempotencyMaintenancePort {
    private val records = ConcurrentHashMap<IdempotencyScope, IdempotencyRecord>()

    override fun reserveOrReplay(
        actorUserId: Long,
        method: String,
        pathTemplate: String,
        idempotencyKey: String,
        requestHash: String,
    ): IdempotencyDecision {
        val scope = IdempotencyScope(actorUserId, method, pathTemplate, idempotencyKey)
        val existing = records[scope]

        if (existing == null) {
            val inserted =
                records.putIfAbsent(scope, IdempotencyRecord(requestHash, IdempotencyState.IN_PROGRESS))
                    ?: return IdempotencyDecision.Reserved
            return evaluateExisting(inserted, requestHash, idempotencyKey)
        }

        return evaluateExisting(existing, requestHash, idempotencyKey)
    }

    override fun complete(
        actorUserId: Long,
        method: String,
        pathTemplate: String,
        idempotencyKey: String,
        status: Int,
        body: Any,
    ) {
        val scope = IdempotencyScope(actorUserId, method, pathTemplate, idempotencyKey)
        val record =
            records[scope]
                ?: throw DomainException(
                    errorCode = ErrorCode.VALIDATION_ERROR,
                    status = 422,
                    message = "Idempotency key not reserved",
                )

        record.state = IdempotencyState.COMPLETED
        record.responseStatus = status
        record.responseBody = body
        records[scope] = record
    }

    override fun markInProgressForTest(
        actorUserId: Long,
        method: String,
        pathTemplate: String,
        idempotencyKey: String,
        requestHash: String,
    ) {
        val scope = IdempotencyScope(actorUserId, method, pathTemplate, idempotencyKey)
        records[scope] =
            IdempotencyRecord(
                requestHash = requestHash,
                state = IdempotencyState.IN_PROGRESS,
            )
    }

    override fun clear() {
        records.clear()
    }

    override fun purgeExpired(nowEpochMillis: Long): Long = 0

    private fun evaluateExisting(
        existing: IdempotencyRecord,
        requestHash: String,
        idempotencyKey: String,
    ): IdempotencyDecision {
        if (existing.requestHash != requestHash) {
            throw DomainException(
                errorCode = ErrorCode.IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD,
                status = 422,
                message = "Idempotency key cannot be reused with different payload",
                details = mapOf("idempotencyKey" to idempotencyKey),
            )
        }

        if (existing.state == IdempotencyState.IN_PROGRESS) {
            throw DomainException(
                errorCode = ErrorCode.IDEMPOTENCY_IN_PROGRESS,
                status = 409,
                message = "Same idempotency key request is in progress",
            )
        }

        return IdempotencyDecision.Replay(
            status = existing.responseStatus ?: 201,
            body = requireNotNull(existing.responseBody),
        )
    }
}
