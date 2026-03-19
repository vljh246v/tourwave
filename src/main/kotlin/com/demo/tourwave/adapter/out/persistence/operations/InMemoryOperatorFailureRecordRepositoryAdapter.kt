package com.demo.tourwave.adapter.out.persistence.operations

import com.demo.tourwave.application.operations.port.OperatorFailureRecordRepository
import com.demo.tourwave.domain.operations.OperatorFailureRecord
import com.demo.tourwave.domain.operations.OperatorFailureSourceType
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Repository
@Profile("!mysql & !mysql-test")
class InMemoryOperatorFailureRecordRepositoryAdapter : OperatorFailureRecordRepository {
    private val sequence = AtomicLong(0)
    private val recordsById = ConcurrentHashMap<Long, OperatorFailureRecord>()
    private val recordIndex = ConcurrentHashMap<String, Long>()

    override fun save(record: OperatorFailureRecord): OperatorFailureRecord {
        val id = record.id ?: sequence.incrementAndGet()
        val saved = record.copy(id = id)
        recordsById[id] = saved
        recordIndex[indexKey(saved.sourceType, saved.sourceKey)] = id
        return saved
    }

    override fun findBySource(sourceType: OperatorFailureSourceType, sourceKey: String): OperatorFailureRecord? =
        recordIndex[indexKey(sourceType, sourceKey)]?.let { recordsById[it] }

    override fun findAll(): List<OperatorFailureRecord> =
        recordsById.values.sortedByDescending { it.updatedAtUtc }

    override fun clear() {
        recordsById.clear()
        recordIndex.clear()
        sequence.set(0)
    }

    private fun indexKey(sourceType: OperatorFailureSourceType, sourceKey: String): String = "${sourceType.name}:$sourceKey"
}
