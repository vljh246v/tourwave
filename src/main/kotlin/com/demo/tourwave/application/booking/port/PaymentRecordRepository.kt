package com.demo.tourwave.application.booking.port

import com.demo.tourwave.domain.payment.PaymentRecord
import com.demo.tourwave.domain.payment.PaymentRecordStatus

interface PaymentRecordRepository {
    fun save(record: PaymentRecord): PaymentRecord
    fun findByBookingId(bookingId: Long): PaymentRecord?
    fun findByStatuses(statuses: Set<PaymentRecordStatus>): List<PaymentRecord>
    fun clear()
}
