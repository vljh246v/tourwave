package com.demo.tourwave.adapter.out.persistence.jpa.payment

import com.demo.tourwave.application.booking.port.PaymentRecordRepository
import com.demo.tourwave.domain.payment.PaymentRecord
import com.demo.tourwave.domain.payment.PaymentRecordStatus
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
@Profile("mysql", "mysql-test")
class JpaPaymentRecordRepositoryAdapter(
    private val paymentRecordJpaRepository: PaymentRecordJpaRepository,
) : PaymentRecordRepository {
    override fun save(record: PaymentRecord): PaymentRecord = paymentRecordJpaRepository.save(record.toEntity()).toDomain()

    override fun findByBookingId(bookingId: Long): PaymentRecord? = paymentRecordJpaRepository.findByBookingId(bookingId)?.toDomain()

    override fun findByStatuses(statuses: Set<PaymentRecordStatus>): List<PaymentRecord> =
        paymentRecordJpaRepository.findByStatusInOrderByUpdatedAtUtcAsc(statuses).map { it.toDomain() }

    override fun findUpdatedBetween(
        startInclusive: Instant,
        endExclusive: Instant,
    ): List<PaymentRecord> =
        paymentRecordJpaRepository
            .findByUpdatedAtUtcGreaterThanEqualAndUpdatedAtUtcLessThanOrderByUpdatedAtUtcAsc(startInclusive, endExclusive)
            .map { it.toDomain() }

    override fun findAll(): List<PaymentRecord> = paymentRecordJpaRepository.findAll().map { it.toDomain() }

    override fun clear() {
        paymentRecordJpaRepository.deleteAllInBatch()
    }
}

private fun PaymentRecord.toEntity(): PaymentRecordJpaEntity =
    PaymentRecordJpaEntity(
        id = id,
        bookingId = bookingId,
        status = status,
        providerName = providerName,
        providerPaymentKey = providerPaymentKey,
        providerAuthorizationId = providerAuthorizationId,
        providerCaptureId = providerCaptureId,
        lastRefundRequestId = lastRefundRequestId,
        lastProviderReference = lastProviderReference,
        lastRefundReasonCode = lastRefundReasonCode,
        lastErrorCode = lastErrorCode,
        refundRetryCount = refundRetryCount,
        lastRefundAttemptedAtUtc = lastRefundAttemptedAtUtc,
        nextRetryAtUtc = nextRetryAtUtc,
        lastRemediationAction = lastRemediationAction,
        lastRemediatedByUserId = lastRemediatedByUserId,
        lastRemediatedAtUtc = lastRemediatedAtUtc,
        lastWebhookEventId = lastWebhookEventId,
        createdAtUtc = createdAtUtc,
        updatedAtUtc = updatedAtUtc,
    )

private fun PaymentRecordJpaEntity.toDomain(): PaymentRecord =
    PaymentRecord(
        id = id,
        bookingId = bookingId,
        status = status,
        providerName = providerName,
        providerPaymentKey = providerPaymentKey,
        providerAuthorizationId = providerAuthorizationId,
        providerCaptureId = providerCaptureId,
        lastRefundRequestId = lastRefundRequestId,
        lastProviderReference = lastProviderReference,
        lastRefundReasonCode = lastRefundReasonCode,
        lastErrorCode = lastErrorCode,
        refundRetryCount = refundRetryCount,
        lastRefundAttemptedAtUtc = lastRefundAttemptedAtUtc,
        nextRetryAtUtc = nextRetryAtUtc,
        lastRemediationAction = lastRemediationAction,
        lastRemediatedByUserId = lastRemediatedByUserId,
        lastRemediatedAtUtc = lastRemediatedAtUtc,
        lastWebhookEventId = lastWebhookEventId,
        createdAtUtc = createdAtUtc,
        updatedAtUtc = updatedAtUtc,
    )
