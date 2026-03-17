package com.demo.tourwave.adapter.out.persistence.jpa.idempotency

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface IdempotencyRecordJpaRepository : JpaRepository<IdempotencyRecordJpaEntity, Long> {
    fun findByActorUserIdAndMethodAndPathTemplateAndIdempotencyKey(
        actorUserId: Long,
        method: String,
        pathTemplate: String,
        idempotencyKey: String
    ): IdempotencyRecordJpaEntity?

    fun deleteByExpiresAtUtcBefore(expiresAtUtc: Instant): Long
}
