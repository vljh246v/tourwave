package com.demo.tourwave.adapter.out.persistence.inquiry

import com.demo.tourwave.application.inquiry.port.InquiryRepository
import com.demo.tourwave.domain.inquiry.Inquiry
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Repository
class InMemoryInquiryRepositoryAdapter : InquiryRepository {
    private val sequence = AtomicLong(0)
    private val inquiries = ConcurrentHashMap<Long, Inquiry>()
    private val inquiryByBooking = ConcurrentHashMap<Long, Long>()

    override fun save(inquiry: Inquiry): Inquiry {
        val inquiryId = inquiry.id ?: sequence.incrementAndGet()
        val saved = inquiry.copy(id = inquiryId)
        inquiries[inquiryId] = saved
        inquiryByBooking[saved.bookingId] = inquiryId
        return saved
    }

    override fun findByBookingId(bookingId: Long): Inquiry? {
        val inquiryId = inquiryByBooking[bookingId] ?: return null
        return inquiries[inquiryId]
    }

    override fun clear() {
        inquiries.clear()
        inquiryByBooking.clear()
        sequence.set(0)
    }
}

