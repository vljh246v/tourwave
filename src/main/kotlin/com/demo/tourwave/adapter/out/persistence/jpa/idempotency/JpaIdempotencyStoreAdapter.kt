package com.demo.tourwave.adapter.out.persistence.jpa.idempotency

import com.demo.tourwave.application.common.port.IdempotencyDecision
import com.demo.tourwave.application.common.port.IdempotencyMaintenancePort
import com.demo.tourwave.application.common.port.IdempotencyStore
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Component
@Profile("mysql", "mysql-test")
@Transactional
class JpaIdempotencyStoreAdapter(
    private val idempotencyRecordJpaRepository: IdempotencyRecordJpaRepository,
    private val objectMapper: ObjectMapper,
    private val clock: Clock,
    @Value("\${tourwave.idempotency.ttl-seconds:86400}")
    private val ttlSeconds: Long
) : IdempotencyStore, IdempotencyMaintenancePort {
    @PersistenceContext
    private lateinit var entityManager: EntityManager

    @Transactional(noRollbackFor = [DataIntegrityViolationException::class])
    override fun reserveOrReplay(
        actorUserId: Long,
        method: String,
        pathTemplate: String,
        idempotencyKey: String,
        requestHash: String
    ): IdempotencyDecision {
        val existing = idempotencyRecordJpaRepository.findByActorUserIdAndMethodAndPathTemplateAndIdempotencyKey(
            actorUserId = actorUserId,
            method = method,
            pathTemplate = pathTemplate,
            idempotencyKey = idempotencyKey
        )
        if (existing != null) {
            return evaluateExisting(existing, requestHash, idempotencyKey)
        }

        val created = IdempotencyRecordJpaEntity(
            actorUserId = actorUserId,
            method = method,
            pathTemplate = pathTemplate,
            idempotencyKey = idempotencyKey,
            requestHash = requestHash,
            state = IdempotencyPersistenceState.IN_PROGRESS,
            createdAtUtc = clock.instant(),
            expiresAtUtc = clock.instant().plusSeconds(ttlSeconds)
        )
        return try {
            idempotencyRecordJpaRepository.saveAndFlush(created)
            IdempotencyDecision.Reserved
        } catch (_: DataIntegrityViolationException) {
            entityManager.clear()
            evaluateExisting(
                requireNotNull(
                    idempotencyRecordJpaRepository.findByActorUserIdAndMethodAndPathTemplateAndIdempotencyKey(
                        actorUserId = actorUserId,
                        method = method,
                        pathTemplate = pathTemplate,
                        idempotencyKey = idempotencyKey
                    )
                ),
                requestHash = requestHash,
                idempotencyKey = idempotencyKey
            )
        }
    }

    override fun complete(
        actorUserId: Long,
        method: String,
        pathTemplate: String,
        idempotencyKey: String,
        status: Int,
        body: Any
    ) {
        val existing = idempotencyRecordJpaRepository.findByActorUserIdAndMethodAndPathTemplateAndIdempotencyKey(
            actorUserId = actorUserId,
            method = method,
            pathTemplate = pathTemplate,
            idempotencyKey = idempotencyKey
        ) ?: throw DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 422,
            message = "Idempotency key not reserved"
        )

        idempotencyRecordJpaRepository.save(
            existing.copy(
                state = IdempotencyPersistenceState.COMPLETED,
                responseStatus = status,
                responseBodyJson = objectMapper.writeValueAsString(body),
                responseBodyType = body.javaClass.name,
                completedAtUtc = clock.instant()
            )
        )
    }

    override fun markInProgressForTest(
        actorUserId: Long,
        method: String,
        pathTemplate: String,
        idempotencyKey: String,
        requestHash: String
    ) {
        val existing = idempotencyRecordJpaRepository.findByActorUserIdAndMethodAndPathTemplateAndIdempotencyKey(
            actorUserId = actorUserId,
            method = method,
            pathTemplate = pathTemplate,
            idempotencyKey = idempotencyKey
        )
        val seed = IdempotencyRecordJpaEntity(
            id = existing?.id,
            actorUserId = actorUserId,
            method = method,
            pathTemplate = pathTemplate,
            idempotencyKey = idempotencyKey,
            requestHash = requestHash,
            state = IdempotencyPersistenceState.IN_PROGRESS,
            responseStatus = null,
            responseBodyJson = null,
            responseBodyType = null,
            createdAtUtc = existing?.createdAtUtc ?: clock.instant(),
            completedAtUtc = null,
            expiresAtUtc = clock.instant().plusSeconds(ttlSeconds)
        )
        idempotencyRecordJpaRepository.save(seed)
    }

    override fun clear() {
        idempotencyRecordJpaRepository.deleteAllInBatch()
    }

    override fun purgeExpired(nowEpochMillis: Long): Long =
        idempotencyRecordJpaRepository.deleteByExpiresAtUtcBefore(java.time.Instant.ofEpochMilli(nowEpochMillis))

    private fun evaluateExisting(
        existing: IdempotencyRecordJpaEntity,
        requestHash: String,
        idempotencyKey: String
    ): IdempotencyDecision {
        if (existing.requestHash != requestHash) {
            throw DomainException(
                errorCode = ErrorCode.IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD,
                status = 422,
                message = "Idempotency key cannot be reused with different payload",
                details = mapOf("idempotencyKey" to idempotencyKey)
            )
        }

        if (existing.state == IdempotencyPersistenceState.IN_PROGRESS) {
            throw DomainException(
                errorCode = ErrorCode.IDEMPOTENCY_IN_PROGRESS,
                status = 409,
                message = "Same idempotency key request is in progress"
            )
        }

        val bodyType = requireNotNull(existing.responseBodyType)
        val rawClass = Class.forName(bodyType)
        val responseBody = objectMapper.readValue(requireNotNull(existing.responseBodyJson), rawClass)
        return IdempotencyDecision.Replay(
            status = existing.responseStatus ?: 201,
            body = responseBody
        )
    }
}
