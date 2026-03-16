package com.demo.tourwave.application.booking.port

import com.demo.tourwave.domain.payment.PaymentRecord

interface PaymentRecordRepository {
    fun save(record: PaymentRecord): PaymentRecord
    fun findByBookingId(bookingId: Long): PaymentRecord?
    fun clear()
}
