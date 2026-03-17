package com.demo.tourwave.domain.booking

import java.time.ZoneId
import java.time.Instant

enum class RefundPolicyAction {
    LEADER_CANCEL,
    OCCURRENCE_CANCEL,
    BOOKING_REJECTED,
    OFFER_DECLINED,
    OFFER_EXPIRED
}

enum class RefundDecisionType {
    FULL_REFUND,
    NO_REFUND,
    REFUND_PENDING
}

enum class RefundReasonCode {
    OCCURRENCE_CANCELED,
    BOOKING_REJECTED,
    OFFER_DECLINED,
    OFFER_EXPIRED,
    LEADER_CANCEL_BEFORE_48_HOURS,
    LEADER_CANCEL_WITHIN_48_HOURS,
    OCCURRENCE_START_TIME_MISSING,
    PAYMENT_NOT_REFUNDABLE
}

data class RefundDecision(
    val type: RefundDecisionType,
    val reasonCode: RefundReasonCode,
    val refundable: Boolean
)

data class RefundPolicyContext(
    val action: RefundPolicyAction,
    val bookingStatus: BookingStatus,
    val paymentStatus: PaymentStatus,
    val occurrenceStartsAtUtc: Instant?,
    val occurrenceTimezone: String = "UTC",
    val evaluatedAtUtc: Instant
)

object BookingRefundPolicy {
    private const val FULL_REFUND_WINDOW_HOURS = 48L

    fun evaluate(context: RefundPolicyContext): RefundDecision {
        if (context.paymentStatus == PaymentStatus.REFUNDED) {
            return RefundDecision(
                type = RefundDecisionType.NO_REFUND,
                reasonCode = RefundReasonCode.PAYMENT_NOT_REFUNDABLE,
                refundable = false
            )
        }

        return when (context.action) {
            RefundPolicyAction.OCCURRENCE_CANCEL -> fullRefund(RefundReasonCode.OCCURRENCE_CANCELED)
            RefundPolicyAction.BOOKING_REJECTED -> fullRefund(RefundReasonCode.BOOKING_REJECTED)
            RefundPolicyAction.OFFER_DECLINED -> fullRefund(RefundReasonCode.OFFER_DECLINED)
            RefundPolicyAction.OFFER_EXPIRED -> fullRefund(RefundReasonCode.OFFER_EXPIRED)
            RefundPolicyAction.LEADER_CANCEL -> evaluateLeaderCancel(context)
        }
    }

    private fun evaluateLeaderCancel(context: RefundPolicyContext): RefundDecision {
        if (context.bookingStatus == BookingStatus.REQUESTED || context.bookingStatus == BookingStatus.WAITLISTED || context.bookingStatus == BookingStatus.OFFERED) {
            return fullRefund(RefundReasonCode.LEADER_CANCEL_BEFORE_48_HOURS)
        }

        val startsAtUtc = context.occurrenceStartsAtUtc
            ?: return RefundDecision(
                type = RefundDecisionType.REFUND_PENDING,
                reasonCode = RefundReasonCode.OCCURRENCE_START_TIME_MISSING,
                refundable = true
            )

        val fullRefundDeadline = startsAtUtc
            .atZone(ZoneId.of(context.occurrenceTimezone))
            .minusHours(FULL_REFUND_WINDOW_HOURS)
            .toInstant()
        return if (!context.evaluatedAtUtc.isAfter(fullRefundDeadline)) {
            fullRefund(RefundReasonCode.LEADER_CANCEL_BEFORE_48_HOURS)
        } else {
            RefundDecision(
                type = RefundDecisionType.NO_REFUND,
                reasonCode = RefundReasonCode.LEADER_CANCEL_WITHIN_48_HOURS,
                refundable = false
            )
        }
    }

    private fun fullRefund(reasonCode: RefundReasonCode): RefundDecision {
        return RefundDecision(
            type = RefundDecisionType.FULL_REFUND,
            reasonCode = reasonCode,
            refundable = true
        )
    }
}
