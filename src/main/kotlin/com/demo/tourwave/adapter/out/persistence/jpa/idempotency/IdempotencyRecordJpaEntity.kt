package com.demo.tourwave.adapter.out.persistence.jpa.idempotency

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

enum class IdempotencyPersistenceState {
    IN_PROGRESS,
    COMPLETED,
}

@Entity
@Table(
    name = "idempotency_records",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_idempotency_scope",
            columnNames = ["actor_user_id", "method", "path_template", "idempotency_key"],
        ),
    ],
    indexes = [Index(name = "idx_idempotency_expires_at", columnList = "expires_at_utc")],
)
data class IdempotencyRecordJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "actor_user_id", nullable = false)
    val actorUserId: Long,
    @Column(nullable = false, length = 16)
    val method: String,
    @Column(name = "path_template", nullable = false, length = 255)
    val pathTemplate: String,
    @Column(name = "idempotency_key", nullable = false, length = 255)
    val idempotencyKey: String,
    @Column(name = "request_hash", nullable = false, length = 64)
    val requestHash: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    val state: IdempotencyPersistenceState,
    @Column(name = "response_status")
    val responseStatus: Int? = null,
    @Column(name = "response_body_json", columnDefinition = "LONGTEXT")
    val responseBodyJson: String? = null,
    @Column(name = "response_body_type", length = 255)
    val responseBodyType: String? = null,
    @Column(name = "created_at_utc", nullable = false)
    val createdAtUtc: Instant,
    @Column(name = "completed_at_utc")
    val completedAtUtc: Instant? = null,
    @Column(name = "expires_at_utc", nullable = false)
    val expiresAtUtc: Instant,
)
