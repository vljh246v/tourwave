package com.demo.tourwave.application.booking

import com.demo.tourwave.application.booking.port.PaymentRecordRepository
import com.demo.tourwave.application.booking.port.RefundExecutionPort
import com.demo.tourwave.application.booking.port.RefundExecutionRequest
import com.demo.tourwave.application.booking.port.RefundExecutionResult
import com.demo.tourwave.application.payment.PaymentAuthorizationRequest
import com.demo.tourwave.application.payment.PaymentCaptureRequest
import com.demo.tourwave.application.payment.PaymentProviderPort
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
import java.time.Instant

class PaymentLedgerService(
    private val paymentRecordRepository: PaymentRecordRepository,
    private val paymentProviderPort: PaymentProviderPort,
    private val refundExecutionPort: RefundExecutionPort,
    private val clock: Clock
) {
    fun initialize(booking: Booking, occurrence: Occurrence, actorUserId: Long): PaymentRecord {
        val now = clock.instant()
        val authorization = paymentProviderPort.authorize(
            PaymentAuthorizationRequest(
                bookingId = requireNotNull(booking.id),
                actorUserId = actorUserId,
                amount = occurrence.unitPrice * booking.partySize,
                currency = occurrence.currency
            )
        )
        return paymentRecordRepository.save(
            PaymentRecord(
                bookingId = requireNotNull(booking.id),
                status = PaymentRecordStatus.AUTHORIZED,
                providerName = authorization.providerName,
                providerPaymentKey = authorization.providerPaymentKey,
                providerAuthorizationId = authorization.authorizationId,
                createdAtUtc = now,
                updatedAtUtc = now
            )
        )
    }

    fun capture(booking: Booking, actorUserId: Long): Booking {
        val existing = paymentRecordRepository.findByBookingId(requireNotNull(booking.id))
        val providerPaymentKey = existing?.providerPaymentKey ?: "legacy-${requireNotNull(booking.id)}"
        val capture = paymentProviderPort.capture(
            PaymentCaptureRequest(
                bookingId = requireNotNull(booking.id),
                actorUserId = actorUserId,
                providerPaymentKey = providerPaymentKey
            )
        )
        upsertRecord(
            bookingId = requireNotNull(booking.id),
            status = PaymentRecordStatus.CAPTURED,
            providerName = capture.providerName,
            providerPaymentKey = providerPaymentKey,
            providerCaptureId = capture.captureId,
            providerReference = capture.providerReference
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
                occurrenceTimezone = occurrence.timezone,
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
                occurrenceTimezone = occurrence.timezone,
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
        val attemptedAt = clock.instant()
        val nextRetryCount = currentRetryCount(bookingId)
        val refundRequestId = "refund-$bookingId-${clock.instant().toEpochMilli()}"
        upsertRecord(
            bookingId = bookingId,
            status = PaymentRecordStatus.REFUND_PENDING,
            refundRequestId = refundRequestId,
            refundReasonCode = decision.reasonCode.name,
            refundRetryCount = nextRetryCount,
            refundAttemptedAtUtc = attemptedAt
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
                    refundReasonCode = decision.reasonCode.name,
                    providerReference = result.externalReference,
                    refundRetryCount = nextRetryCount,
                    refundAttemptedAtUtc = attemptedAt
                )
                booking.copy(paymentStatus = PaymentStatus.REFUNDED)
            }

            is RefundExecutionResult.RetryableFailure -> {
                upsertRecord(
                    bookingId = bookingId,
                    status = PaymentRecordStatus.REFUND_FAILED_RETRYABLE,
                    refundRequestId = refundRequestId,
                    refundReasonCode = decision.reasonCode.name,
                    errorCode = result.errorCode,
                    refundRetryCount = nextRetryCount,
                    refundAttemptedAtUtc = attemptedAt
                )
                booking.copy(paymentStatus = PaymentStatus.REFUND_PENDING)
            }

            is RefundExecutionResult.ReviewRequired -> {
                upsertRecord(
                    bookingId = bookingId,
                    status = PaymentRecordStatus.REFUND_REVIEW_REQUIRED,
                    refundRequestId = refundRequestId,
                    refundReasonCode = decision.reasonCode.name,
                    errorCode = result.errorCode,
                    refundRetryCount = nextRetryCount,
                    refundAttemptedAtUtc = attemptedAt
                )
                booking.copy(paymentStatus = PaymentStatus.REFUND_PENDING)
            }
        }
    }

    private fun upsertRecord(
        bookingId: Long,
        status: PaymentRecordStatus,
        providerName: String? = null,
        providerPaymentKey: String? = null,
        providerAuthorizationId: String? = null,
        providerCaptureId: String? = null,
        refundRequestId: String? = null,
        providerReference: String? = null,
        refundReasonCode: String? = null,
        errorCode: String? = null,
        refundRetryCount: Int? = null,
        refundAttemptedAtUtc: Instant? = null,
        webhookEventId: String? = null
    ): PaymentRecord {
        val now = clock.instant()
        val existing = paymentRecordRepository.findByBookingId(bookingId)
        val record = if (existing == null) {
            PaymentRecord(
                bookingId = bookingId,
                status = status,
                providerName = providerName,
                providerPaymentKey = providerPaymentKey,
                providerAuthorizationId = providerAuthorizationId,
                providerCaptureId = providerCaptureId,
                lastRefundRequestId = refundRequestId,
                lastProviderReference = providerReference,
                lastRefundReasonCode = refundReasonCode,
                lastErrorCode = errorCode,
                refundRetryCount = refundRetryCount ?: 0,
                lastRefundAttemptedAtUtc = refundAttemptedAtUtc,
                lastWebhookEventId = webhookEventId,
                createdAtUtc = now,
                updatedAtUtc = now
            )
        } else {
            existing.copy(
                status = status,
                providerName = providerName ?: existing.providerName,
                providerPaymentKey = providerPaymentKey ?: existing.providerPaymentKey,
                providerAuthorizationId = providerAuthorizationId ?: existing.providerAuthorizationId,
                providerCaptureId = providerCaptureId ?: existing.providerCaptureId,
                lastRefundRequestId = refundRequestId ?: existing.lastRefundRequestId,
                lastProviderReference = providerReference ?: existing.lastProviderReference,
                lastRefundReasonCode = refundReasonCode ?: existing.lastRefundReasonCode,
                lastErrorCode = errorCode,
                refundRetryCount = refundRetryCount ?: existing.refundRetryCount,
                lastRefundAttemptedAtUtc = refundAttemptedAtUtc ?: existing.lastRefundAttemptedAtUtc,
                lastWebhookEventId = webhookEventId ?: existing.lastWebhookEventId,
                updatedAtUtc = now
            )
        }
        return paymentRecordRepository.save(record)
    }

    fun applyWebhookStatus(
        bookingId: Long,
        status: PaymentRecordStatus,
        providerName: String,
        providerReference: String? = null,
        providerCaptureId: String? = null,
        providerAuthorizationId: String? = null,
        errorCode: String? = null,
        webhookEventId: String
    ): PaymentRecord {
        return upsertRecord(
            bookingId = bookingId,
            status = status,
            providerName = providerName,
            providerAuthorizationId = providerAuthorizationId,
            providerCaptureId = providerCaptureId,
            providerReference = providerReference,
            errorCode = errorCode,
            webhookEventId = webhookEventId
        )
    }

    private fun currentRetryCount(bookingId: Long): Int {
        return paymentRecordRepository.findByBookingId(bookingId)?.refundRetryCount?.plus(1) ?: 1
    }
}
