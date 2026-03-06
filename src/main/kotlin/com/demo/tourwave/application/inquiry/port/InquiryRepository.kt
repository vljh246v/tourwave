package com.demo.tourwave.application.inquiry.port

import com.demo.tourwave.domain.inquiry.Inquiry
import com.demo.tourwave.domain.inquiry.InquiryMessage

interface InquiryRepository {
    fun save(inquiry: Inquiry): Inquiry
    fun findById(inquiryId: Long): Inquiry?
    fun findByBookingId(bookingId: Long): Inquiry?
    fun saveMessage(message: InquiryMessage): InquiryMessage
    fun findMessagesByInquiryId(inquiryId: Long): List<InquiryMessage>
    fun clear()
}
