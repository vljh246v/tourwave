package com.demo.tourwave.adapter.out.persistence.inquiry

import com.demo.tourwave.application.inquiry.port.InquiryRepository
import com.demo.tourwave.domain.inquiry.Inquiry
import com.demo.tourwave.domain.inquiry.InquiryMessage
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Repository
class InMemoryInquiryRepositoryAdapter : InquiryRepository {
    private val inquirySequence = AtomicLong(0)
    private val messageSequence = AtomicLong(0)
    private val inquiries = ConcurrentHashMap<Long, Inquiry>()
    private val inquiryByBooking = ConcurrentHashMap<Long, Long>()
    private val messagesByInquiry = ConcurrentHashMap<Long, MutableList<InquiryMessage>>()

    override fun save(inquiry: Inquiry): Inquiry {
        val inquiryId = inquiry.id ?: inquirySequence.incrementAndGet()
        val saved = inquiry.copy(id = inquiryId)
        inquiries[inquiryId] = saved
        inquiryByBooking[saved.bookingId] = inquiryId
        return saved
    }

    override fun findById(inquiryId: Long): Inquiry? {
        return inquiries[inquiryId]
    }

    override fun findByBookingId(bookingId: Long): Inquiry? {
        val inquiryId = inquiryByBooking[bookingId] ?: return null
        return inquiries[inquiryId]
    }

    override fun findByCreatedByUserId(createdByUserId: Long): List<Inquiry> {
        return inquiries.values
            .filter { it.createdByUserId == createdByUserId }
            .sortedWith(compareByDescending<Inquiry> { it.createdAt }.thenByDescending { it.id ?: Long.MIN_VALUE })
    }

    override fun saveMessage(message: InquiryMessage): InquiryMessage {
        val messageId = message.id ?: messageSequence.incrementAndGet()
        val saved = message.copy(id = messageId)
        messagesByInquiry.computeIfAbsent(saved.inquiryId) { mutableListOf() }.add(saved)
        return saved
    }

    override fun findMessagesByInquiryId(inquiryId: Long): List<InquiryMessage> {
        return messagesByInquiry[inquiryId]
            ?.sortedBy { it.id }
            ?.toList()
            ?: emptyList()
    }

    override fun clear() {
        inquiries.clear()
        inquiryByBooking.clear()
        messagesByInquiry.clear()
        inquirySequence.set(0)
        messageSequence.set(0)
    }
}
