package com.demo.tourwave.application.inquiry.port

import com.demo.tourwave.domain.inquiry.Inquiry
import com.demo.tourwave.domain.inquiry.InquiryMessage

interface InquiryRepository {
    fun save(inquiry: Inquiry): Inquiry

    fun findById(inquiryId: Long): Inquiry?

    fun findByBookingId(bookingId: Long): Inquiry?

    fun findByCreatedByUserId(createdByUserId: Long): List<Inquiry>

    fun saveMessage(message: InquiryMessage): InquiryMessage

    fun findMessageById(messageId: Long): InquiryMessage?

    fun findMessagesByInquiryId(inquiryId: Long): List<InquiryMessage>

    fun clear()
}
