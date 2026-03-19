package com.demo.tourwave.adapter.out.persistence.jpa.operations

import com.demo.tourwave.application.operations.port.OperatorFailureRecordRepository
import com.demo.tourwave.domain.operations.OperatorFailureRecord
import com.demo.tourwave.domain.operations.OperatorFailureSourceType
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository

@Repository
@Profile("mysql", "mysql-test")
class JpaOperatorFailureRecordRepositoryAdapter(
    private val operatorFailureRecordJpaRepository: OperatorFailureRecordJpaRepository
) : OperatorFailureRecordRepository {
    override fun save(record: OperatorFailureRecord): OperatorFailureRecord =
        operatorFailureRecordJpaRepository.save(record.toEntity()).toDomain()

    override fun findBySource(sourceType: OperatorFailureSourceType, sourceKey: String): OperatorFailureRecord? =
        operatorFailureRecordJpaRepository.findBySourceTypeAndSourceKey(sourceType, sourceKey)?.toDomain()

    override fun findAll(): List<OperatorFailureRecord> =
        operatorFailureRecordJpaRepository.findAllByOrderByUpdatedAtUtcDesc().map { it.toDomain() }

    override fun clear() {
        operatorFailureRecordJpaRepository.deleteAllInBatch()
    }
}

private fun OperatorFailureRecord.toEntity(): OperatorFailureRecordJpaEntity =
    OperatorFailureRecordJpaEntity(
        id = id,
        sourceType = sourceType,
        sourceKey = sourceKey,
        status = status,
        lastAction = lastAction,
        note = note,
        lastActionByUserId = lastActionByUserId,
        lastActionAtUtc = lastActionAtUtc,
        retryCount = retryCount,
        createdAtUtc = createdAtUtc,
        updatedAtUtc = updatedAtUtc
    )

private fun OperatorFailureRecordJpaEntity.toDomain(): OperatorFailureRecord =
    OperatorFailureRecord(
        id = id,
        sourceType = sourceType,
        sourceKey = sourceKey,
        status = status,
        lastAction = lastAction,
        note = note,
        lastActionByUserId = lastActionByUserId,
        lastActionAtUtc = lastActionAtUtc,
        retryCount = retryCount,
        createdAtUtc = createdAtUtc,
        updatedAtUtc = updatedAtUtc
    )
