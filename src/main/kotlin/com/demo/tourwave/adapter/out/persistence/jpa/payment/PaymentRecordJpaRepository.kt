package com.demo.tourwave.adapter.out.persistence.jpa.payment

import com.demo.tourwave.domain.payment.PaymentRecordStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface PaymentRecordJpaRepository : JpaRepository<PaymentRecordJpaEntity, Long> {
    fun findByBookingId(bookingId: Long): PaymentRecordJpaEntity?

    fun findByStatusInOrderByUpdatedAtUtcAsc(statuses: Collection<PaymentRecordStatus>): List<PaymentRecordJpaEntity>

    fun findByUpdatedAtUtcGreaterThanEqualAndUpdatedAtUtcLessThanOrderByUpdatedAtUtcAsc(
        startInclusive: Instant,
        endExclusive: Instant,
    ): List<PaymentRecordJpaEntity>
}
