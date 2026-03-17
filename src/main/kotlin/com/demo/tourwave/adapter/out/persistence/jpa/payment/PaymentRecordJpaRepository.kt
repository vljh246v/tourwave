package com.demo.tourwave.adapter.out.persistence.jpa.payment

import com.demo.tourwave.domain.payment.PaymentRecordStatus
import org.springframework.data.jpa.repository.JpaRepository

interface PaymentRecordJpaRepository : JpaRepository<PaymentRecordJpaEntity, Long> {
    fun findByBookingId(bookingId: Long): PaymentRecordJpaEntity?
    fun findByStatusInOrderByUpdatedAtUtcAsc(statuses: Collection<PaymentRecordStatus>): List<PaymentRecordJpaEntity>
}
