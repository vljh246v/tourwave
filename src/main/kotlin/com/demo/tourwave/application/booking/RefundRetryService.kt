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

data class RefundRetryJobResult(
    val scannedCount: Int,
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
    private val clock: Clock
) {
    fun retryPendingRefunds(): RefundRetryJobResult {
        val records = paymentRecordRepository.findByStatuses(setOf(PaymentRecordStatus.REFUND_FAILED_RETRYABLE))
        var refundedCount = 0
        var reviewRequiredCount = 0
        var stillRetryableCount = 0

        records.forEach { record ->
            val booking = bookingRepository.findById(record.bookingId) ?: return@forEach
            val requestId = record.lastRefundRequestId ?: "retry-${record.bookingId}-${clock.instant().toEpochMilli()}"
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
                            lastErrorCode = null,
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
                            updatedAtUtc = clock.instant()
                        )
                    )
                }

                is RefundExecutionResult.RetryableFailure -> {
                    stillRetryableCount += 1
                    paymentRecordRepository.save(
                        record.copy(
                            status = PaymentRecordStatus.REFUND_FAILED_RETRYABLE,
                            lastRefundRequestId = requestId,
                            lastErrorCode = result.errorCode,
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
            refundedCount = refundedCount,
            reviewRequiredCount = reviewRequiredCount,
            stillRetryableCount = stillRetryableCount
        )
    }
}
