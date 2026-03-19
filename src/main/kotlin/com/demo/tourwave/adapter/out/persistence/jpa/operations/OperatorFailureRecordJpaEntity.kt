package com.demo.tourwave.adapter.out.persistence.jpa.operations

import com.demo.tourwave.domain.operations.OperatorFailureAction
import com.demo.tourwave.domain.operations.OperatorFailureRecordStatus
import com.demo.tourwave.domain.operations.OperatorFailureSourceType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(
    name = "operator_failure_records",
    indexes = [
        Index(name = "uk_operator_failure_records_source", columnList = "source_type,source_key", unique = true),
        Index(name = "idx_operator_failure_records_updated_at_utc", columnList = "updated_at_utc")
    ]
)
data class OperatorFailureRecordJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 64)
    val sourceType: OperatorFailureSourceType,
    @Column(name = "source_key", nullable = false, length = 191)
    val sourceKey: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    val status: OperatorFailureRecordStatus,
    @Enumerated(EnumType.STRING)
    @Column(name = "last_action", nullable = false, length = 32)
    val lastAction: OperatorFailureAction,
    @Column(name = "note", length = 500)
    val note: String? = null,
    @Column(name = "last_action_by_user_id", nullable = false)
    val lastActionByUserId: Long,
    @Column(name = "last_action_at_utc", nullable = false)
    val lastActionAtUtc: Instant,
    @Column(name = "retry_count", nullable = false)
    val retryCount: Int,
    @Column(name = "created_at_utc", nullable = false)
    val createdAtUtc: Instant,
    @Column(name = "updated_at_utc", nullable = false)
    val updatedAtUtc: Instant
)
