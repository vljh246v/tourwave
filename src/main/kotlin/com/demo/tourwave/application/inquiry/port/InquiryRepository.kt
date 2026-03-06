package com.demo.tourwave.application.inquiry.port

import com.demo.tourwave.domain.inquiry.Inquiry

interface InquiryRepository {
    fun save(inquiry: Inquiry): Inquiry
    fun findByBookingId(bookingId: Long): Inquiry?
    fun clear()
}

