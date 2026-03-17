package com.demo.tourwave.adapter.out.persistence.jpa.payment

import com.demo.tourwave.application.booking.port.PaymentRecordRepository
import com.demo.tourwave.domain.payment.PaymentRecord
import com.demo.tourwave.domain.payment.PaymentRecordStatus
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository

@Repository
@Profile("mysql", "mysql-test")
class JpaPaymentRecordRepositoryAdapter(
    private val paymentRecordJpaRepository: PaymentRecordJpaRepository
) : PaymentRecordRepository {
    override fun save(record: PaymentRecord): PaymentRecord =
        paymentRecordJpaRepository.save(record.toEntity()).toDomain()

    override fun findByBookingId(bookingId: Long): PaymentRecord? =
        paymentRecordJpaRepository.findByBookingId(bookingId)?.toDomain()

    override fun findByStatuses(statuses: Set<PaymentRecordStatus>): List<PaymentRecord> =
        paymentRecordJpaRepository.findByStatusInOrderByUpdatedAtUtcAsc(statuses).map { it.toDomain() }

    override fun clear() {
        paymentRecordJpaRepository.deleteAllInBatch()
    }
}

private fun PaymentRecord.toEntity(): PaymentRecordJpaEntity =
    PaymentRecordJpaEntity(
        id = id,
        bookingId = bookingId,
        status = status,
        lastRefundRequestId = lastRefundRequestId,
        lastRefundReasonCode = lastRefundReasonCode,
        lastErrorCode = lastErrorCode,
        createdAtUtc = createdAtUtc,
        updatedAtUtc = updatedAtUtc
    )

private fun PaymentRecordJpaEntity.toDomain(): PaymentRecord =
    PaymentRecord(
        id = id,
        bookingId = bookingId,
        status = status,
        lastRefundRequestId = lastRefundRequestId,
        lastRefundReasonCode = lastRefundReasonCode,
        lastErrorCode = lastErrorCode,
        createdAtUtc = createdAtUtc,
        updatedAtUtc = updatedAtUtc
    )
