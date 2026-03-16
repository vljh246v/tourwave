package com.demo.tourwave.adapter.out.persistence.payment

import com.demo.tourwave.application.booking.port.PaymentRecordRepository
import com.demo.tourwave.domain.payment.PaymentRecord
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Repository
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

    override fun clear() {
        recordsById.clear()
        bookingToRecordId.clear()
        sequence.set(0)
    }
}
