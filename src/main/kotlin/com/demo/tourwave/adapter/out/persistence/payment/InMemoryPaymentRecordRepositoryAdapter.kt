package com.demo.tourwave.adapter.out.persistence.payment

import com.demo.tourwave.application.booking.port.PaymentRecordRepository
import com.demo.tourwave.domain.payment.PaymentRecord
import com.demo.tourwave.domain.payment.PaymentRecordStatus
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Repository
@Profile("!mysql & !mysql-test")
class InMemoryPaymentRecordRepositoryAdapter : PaymentRecordRepository {
    private val sequence = AtomicLong(0)
    private val recordsById = ConcurrentHashMap<Long, PaymentRecord>()
    private val bookingToRecordId = ConcurrentHashMap<Long, Long>()

    override fun save(record: PaymentRecord): PaymentRecord {
        val id = record.id ?: sequence.incrementAndGet()
        val saved = record.copy(id = id)
        recordsById[id] = saved
        bookingToRecordId[saved.bookingId] = id
        return saved
    }

    override fun findByBookingId(bookingId: Long): PaymentRecord? {
        val recordId = bookingToRecordId[bookingId] ?: return null
        return recordsById[recordId]
    }

    override fun findByStatuses(statuses: Set<PaymentRecordStatus>): List<PaymentRecord> {
        return recordsById.values
            .filter { it.status in statuses }
            .sortedBy { it.updatedAtUtc }
    }

    override fun findUpdatedBetween(
        startInclusive: Instant,
        endExclusive: Instant,
    ): List<PaymentRecord> {
        return recordsById.values
            .filter { !it.updatedAtUtc.isBefore(startInclusive) && it.updatedAtUtc.isBefore(endExclusive) }
            .sortedBy { it.updatedAtUtc }
    }

    override fun findAll(): List<PaymentRecord> {
        return recordsById.values.sortedBy { it.id ?: Long.MAX_VALUE }
    }

    override fun clear() {
        recordsById.clear()
        bookingToRecordId.clear()
        sequence.set(0)
    }
}
