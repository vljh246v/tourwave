package com.demo.tourwave.application.booking

import com.demo.tourwave.application.booking.port.PaymentRecordRepository
import com.demo.tourwave.application.booking.port.RefundExecutionPort
import com.demo.tourwave.application.booking.port.RefundExecutionRequest
import com.demo.tourwave.application.booking.port.RefundExecutionResult
import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingRefundPolicy
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.booking.RefundDecision
import com.demo.tourwave.domain.booking.RefundPolicyAction
import com.demo.tourwave.domain.booking.RefundPolicyContext
import com.demo.tourwave.domain.occurrence.Occurrence
import com.demo.tourwave.domain.payment.PaymentRecord
import com.demo.tourwave.domain.payment.PaymentRecordStatus
import java.time.Clock

class PaymentLedgerService(
    private val paymentRecordRepository: PaymentRecordRepository,
    private val refundExecutionPort: RefundExecutionPort,
    private val clock: Clock
) {
    fun initialize(booking: Booking): PaymentRecord {
        val now = clock.instant()
        return paymentRecordRepository.save(
            PaymentRecord(
                bookingId = requireNotNull(booking.id),
                status = PaymentRecordStatus.AUTHORIZED,
                createdAtUtc = now,
                updatedAtUtc = now
            )
        )
    }

    fun capture(booking: Booking): Booking {
        upsertRecord(
            bookingId = requireNotNull(booking.id),
            status = PaymentRecordStatus.CAPTURED
        )
        return booking.copy(paymentStatus = PaymentStatus.PAID)
    }

    fun applyRefundPolicy(
        booking: Booking,
        occurrence: Occurrence,
        action: RefundPolicyAction,
        actorUserId: Long
    ): Booking {
        val decision = BookingRefundPolicy.evaluate(
            RefundPolicyContext(
                action = action,
                bookingStatus = booking.status,
                paymentStatus = booking.paymentStatus,
                occurrenceStartsAtUtc = occurrence.startsAtUtc,
                evaluatedAtUtc = clock.instant()
            )
        )

        return applyDecision(
            booking = booking,
            decision = decision,
            actorUserId = actorUserId
        )
    }

    fun decisionFor(
        booking: Booking,
        occurrence: Occurrence,
        action: RefundPolicyAction
    ): RefundDecision {
        return BookingRefundPolicy.evaluate(
            RefundPolicyContext(
                action = action,
                bookingStatus = booking.status,
                paymentStatus = booking.paymentStatus,
                occurrenceStartsAtUtc = occurrence.startsAtUtc,
                evaluatedAtUtc = clock.instant()
            )
        )
    }

    fun currentRecord(bookingId: Long): PaymentRecord? {
        return paymentRecordRepository.findByBookingId(bookingId)
    }

    private fun applyDecision(
        booking: Booking,
        decision: RefundDecision,
        actorUserId: Long
    ): Booking {
        return when (decision.type) {
            com.demo.tourwave.domain.booking.RefundDecisionType.NO_REFUND -> {
                upsertRecord(
                    bookingId = requireNotNull(booking.id),
                    status = PaymentRecordStatus.NO_REFUND,
                    refundReasonCode = decision.reasonCode.name
                )
                booking
            }

            com.demo.tourwave.domain.booking.RefundDecisionType.REFUND_PENDING -> {
                upsertRecord(
                    bookingId = requireNotNull(booking.id),
                    status = PaymentRecordStatus.REFUND_PENDING,
                    refundReasonCode = decision.reasonCode.name
                )
                booking.copy(paymentStatus = PaymentStatus.REFUND_PENDING)
            }

            com.demo.tourwave.domain.booking.RefundDecisionType.FULL_REFUND -> executeRefund(
                booking = booking,
                decision = decision,
                actorUserId = actorUserId
            )
        }
    }

    private fun executeRefund(
        booking: Booking,
        decision: RefundDecision,
        actorUserId: Long
    ): Booking {
        val bookingId = requireNotNull(booking.id)
        val refundRequestId = "refund-$bookingId-${clock.instant().toEpochMilli()}"
        upsertRecord(
            bookingId = bookingId,
            status = PaymentRecordStatus.REFUND_PENDING,
            refundRequestId = refundRequestId,
            refundReasonCode = decision.reasonCode.name
        )

        return when (
            val result = refundExecutionPort.executeRefund(
                RefundExecutionRequest(
                    bookingId = bookingId,
                    actorUserId = actorUserId,
                    refundRequestId = refundRequestId,
                    reasonCode = decision.reasonCode
                )
            )
        ) {
            is RefundExecutionResult.Success -> {
                upsertRecord(
                    bookingId = bookingId,
                    status = PaymentRecordStatus.REFUNDED,
                    refundRequestId = refundRequestId,
                    refundReasonCode = decision.reasonCode.name
                )
                booking.copy(paymentStatus = PaymentStatus.REFUNDED)
            }

            is RefundExecutionResult.RetryableFailure -> {
                upsertRecord(
                    bookingId = bookingId,
                    status = PaymentRecordStatus.REFUND_FAILED_RETRYABLE,
                    refundRequestId = refundRequestId,
                    refundReasonCode = decision.reasonCode.name,
                    errorCode = result.errorCode
                )
                booking.copy(paymentStatus = PaymentStatus.REFUND_PENDING)
            }

            is RefundExecutionResult.ReviewRequired -> {
                upsertRecord(
                    bookingId = bookingId,
                    status = PaymentRecordStatus.REFUND_REVIEW_REQUIRED,
                    refundRequestId = refundRequestId,
                    refundReasonCode = decision.reasonCode.name,
                    errorCode = result.errorCode
                )
                booking.copy(paymentStatus = PaymentStatus.REFUND_PENDING)
            }
        }
    }

    private fun upsertRecord(
        bookingId: Long,
        status: PaymentRecordStatus,
        refundRequestId: String? = null,
        refundReasonCode: String? = null,
        errorCode: String? = null
    ): PaymentRecord {
        val now = clock.instant()
        val existing = paymentRecordRepository.findByBookingId(bookingId)
        val record = if (existing == null) {
            PaymentRecord(
                bookingId = bookingId,
                status = status,
                lastRefundRequestId = refundRequestId,
                lastRefundReasonCode = refundReasonCode,
                lastErrorCode = errorCode,
                createdAtUtc = now,
                updatedAtUtc = now
            )
        } else {
            existing.copy(
                status = status,
                lastRefundRequestId = refundRequestId ?: existing.lastRefundRequestId,
                lastRefundReasonCode = refundReasonCode ?: existing.lastRefundReasonCode,
                lastErrorCode = errorCode,
                updatedAtUtc = now
            )
        }
        return paymentRecordRepository.save(record)
    }
}
