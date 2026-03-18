package com.demo.tourwave.application.booking

import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.PaymentRecordRepository
import com.demo.tourwave.application.booking.port.RefundExecutionPort
import com.demo.tourwave.application.booking.port.RefundExecutionRequest
import com.demo.tourwave.application.booking.port.RefundExecutionResult
import com.demo.tourwave.application.common.port.AuditEventCommand
import com.demo.tourwave.application.common.port.AuditEventPort
import com.demo.tourwave.domain.payment.PaymentRecordStatus
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.time.Instant

data class RefundRetryJobResult(
    val scannedCount: Int,
    val eligibleCount: Int,
    val refundedCount: Int,
    val reviewRequiredCount: Int,
    val stillRetryableCount: Int
)

@Transactional
class RefundRetryService(
    private val paymentRecordRepository: PaymentRecordRepository,
    private val bookingRepository: BookingRepository,
    private val refundExecutionPort: RefundExecutionPort,
    private val auditEventPort: AuditEventPort,
    private val maxRetryAttempts: Int,
    private val retryCooldown: Duration,
    private val clock: Clock
) {
    fun retryPendingRefunds(): RefundRetryJobResult {
        val records = paymentRecordRepository.findByStatuses(setOf(PaymentRecordStatus.REFUND_FAILED_RETRYABLE))
        val eligibleRecords = records.filter { isEligibleForRetry(it) }
        var refundedCount = 0
        var reviewRequiredCount = 0
        var stillRetryableCount = 0

        eligibleRecords.forEach { record ->
            val booking = bookingRepository.findById(record.bookingId) ?: return@forEach
            val requestId = record.lastRefundRequestId ?: "retry-${record.bookingId}-${clock.instant().toEpochMilli()}"
            val nextRetryCount = record.refundRetryCount + 1
            val attemptedAt = clock.instant()
            when (
                val result = refundExecutionPort.executeRefund(
                    RefundExecutionRequest(
                        bookingId = record.bookingId,
                        actorUserId = 0L,
                        refundRequestId = requestId,
                        reasonCode = com.demo.tourwave.domain.booking.RefundReasonCode.valueOf(
                            record.lastRefundReasonCode ?: "UNKNOWN"
                        )
                    )
                )
            ) {
                is RefundExecutionResult.Success -> {
                    refundedCount += 1
                    paymentRecordRepository.save(
                        record.copy(
                            status = PaymentRecordStatus.REFUNDED,
                            lastRefundRequestId = requestId,
                            lastProviderReference = result.externalReference,
                            lastErrorCode = null,
                            refundRetryCount = nextRetryCount,
                            lastRefundAttemptedAtUtc = attemptedAt,
                            nextRetryAtUtc = null,
                            updatedAtUtc = clock.instant()
                        )
                    )
                    bookingRepository.save(booking.copy(paymentStatus = com.demo.tourwave.domain.booking.PaymentStatus.REFUNDED))
                }

                is RefundExecutionResult.ReviewRequired -> {
                    reviewRequiredCount += 1
                    paymentRecordRepository.save(
                        record.copy(
                            status = PaymentRecordStatus.REFUND_REVIEW_REQUIRED,
                            lastRefundRequestId = requestId,
                            lastErrorCode = result.errorCode,
                            refundRetryCount = nextRetryCount,
                            lastRefundAttemptedAtUtc = attemptedAt,
                            nextRetryAtUtc = null,
                            updatedAtUtc = clock.instant()
                        )
                    )
                }

                is RefundExecutionResult.RetryableFailure -> {
                    stillRetryableCount += 1
                    paymentRecordRepository.save(
                        record.copy(
                            status = if (nextRetryCount >= maxRetryAttempts) {
                                PaymentRecordStatus.REFUND_REVIEW_REQUIRED
                            } else {
                                PaymentRecordStatus.REFUND_FAILED_RETRYABLE
                            },
                            lastRefundRequestId = requestId,
                            lastErrorCode = result.errorCode,
                            refundRetryCount = nextRetryCount,
                            lastRefundAttemptedAtUtc = attemptedAt,
                            nextRetryAtUtc = nextRetryAt(attemptedAt, nextRetryCount),
                            updatedAtUtc = clock.instant()
                        )
                    )
                }
            }

            auditEventPort.append(
                AuditEventCommand(
                    actor = "JOB:0",
                    action = "REFUND_RETRY_PROCESSED",
                    resourceType = "PAYMENT_RECORD",
                    resourceId = requireNotNull(record.id),
                    occurredAtUtc = clock.instant(),
                    reasonCode = "REFUND_RETRY",
                    afterJson = mapOf(
                        "bookingId" to record.bookingId,
                        "status" to paymentRecordRepository.findByBookingId(record.bookingId)?.status?.name
                    )
                )
            )
        }

        return RefundRetryJobResult(
            scannedCount = records.size,
            eligibleCount = eligibleRecords.size,
            refundedCount = refundedCount,
            reviewRequiredCount = reviewRequiredCount,
            stillRetryableCount = stillRetryableCount
        )
    }

    fun retryBookingRefund(bookingId: Long): PaymentRecordStatus {
        val record = paymentRecordRepository.findByBookingId(bookingId)
            ?: throw IllegalArgumentException("Payment record not found for booking $bookingId")
        require(record.status == PaymentRecordStatus.REFUND_FAILED_RETRYABLE || record.status == PaymentRecordStatus.REFUND_REVIEW_REQUIRED) {
            "Booking $bookingId is not waiting for refund remediation"
        }
        if (record.refundRetryCount >= maxRetryAttempts) {
            paymentRecordRepository.save(
                record.copy(
                    status = PaymentRecordStatus.REFUND_REVIEW_REQUIRED,
                    updatedAtUtc = clock.instant()
                )
            )
            return PaymentRecordStatus.REFUND_REVIEW_REQUIRED
        }

        val booking = bookingRepository.findById(bookingId) ?: throw IllegalArgumentException("Booking not found: $bookingId")
        val requestId = record.lastRefundRequestId ?: "manual-$bookingId-${clock.instant().toEpochMilli()}"
        val nextRetryCount = record.refundRetryCount + 1
        val attemptedAt = clock.instant()
        val updatedStatus = when (
            val result = refundExecutionPort.executeRefund(
                RefundExecutionRequest(
                    bookingId = bookingId,
                    actorUserId = 0L,
                    refundRequestId = requestId,
                    reasonCode = com.demo.tourwave.domain.booking.RefundReasonCode.valueOf(record.lastRefundReasonCode ?: "UNKNOWN")
                )
            )
        ) {
            is RefundExecutionResult.Success -> {
                paymentRecordRepository.save(
                    record.copy(
                        status = PaymentRecordStatus.REFUNDED,
                        lastRefundRequestId = requestId,
                        lastProviderReference = result.externalReference,
                        lastErrorCode = null,
                        refundRetryCount = nextRetryCount,
                        lastRefundAttemptedAtUtc = attemptedAt,
                        nextRetryAtUtc = null,
                        updatedAtUtc = clock.instant()
                    )
                )
                bookingRepository.save(booking.copy(paymentStatus = com.demo.tourwave.domain.booking.PaymentStatus.REFUNDED))
                PaymentRecordStatus.REFUNDED
            }

            is RefundExecutionResult.ReviewRequired -> {
                paymentRecordRepository.save(
                    record.copy(
                        status = PaymentRecordStatus.REFUND_REVIEW_REQUIRED,
                        lastRefundRequestId = requestId,
                        lastErrorCode = result.errorCode,
                        refundRetryCount = nextRetryCount,
                        lastRefundAttemptedAtUtc = attemptedAt,
                        nextRetryAtUtc = null,
                        updatedAtUtc = clock.instant()
                    )
                )
                PaymentRecordStatus.REFUND_REVIEW_REQUIRED
            }

            is RefundExecutionResult.RetryableFailure -> {
                val resolvedStatus = if (nextRetryCount >= maxRetryAttempts) {
                    PaymentRecordStatus.REFUND_REVIEW_REQUIRED
                } else {
                    PaymentRecordStatus.REFUND_FAILED_RETRYABLE
                }
                paymentRecordRepository.save(
                    record.copy(
                        status = resolvedStatus,
                        lastRefundRequestId = requestId,
                        lastErrorCode = result.errorCode,
                        refundRetryCount = nextRetryCount,
                        lastRefundAttemptedAtUtc = attemptedAt,
                        nextRetryAtUtc = nextRetryAt(attemptedAt, nextRetryCount),
                        updatedAtUtc = clock.instant()
                    )
                )
                resolvedStatus
            }
        }
        return updatedStatus
    }

    private fun nextRetryAt(attemptedAt: Instant, nextRetryCount: Int): Instant? {
        return if (nextRetryCount >= maxRetryAttempts) {
            null
        } else {
            attemptedAt.plus(retryCooldown)
        }
    }

    private fun isEligibleForRetry(record: com.demo.tourwave.domain.payment.PaymentRecord): Boolean {
        if (record.refundRetryCount >= maxRetryAttempts) {
            return false
        }
        val attemptedAt = record.lastRefundAttemptedAtUtc ?: return true
        return !attemptedAt.plus(retryCooldown).isAfter(clock.instant())
    }
}
