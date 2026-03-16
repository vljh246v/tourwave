package com.demo.tourwave.domain.booking

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import java.time.Instant

class BookingRefundPolicyTest {
    @Test
    fun `leader cancel before 48 hours gets full refund`() {
        val decision = BookingRefundPolicy.evaluate(
            RefundPolicyContext(
                action = RefundPolicyAction.LEADER_CANCEL,
                bookingStatus = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                occurrenceStartsAtUtc = Instant.parse("2026-03-20T12:00:00Z"),
                evaluatedAtUtc = Instant.parse("2026-03-18T12:00:00Z")
            )
        )

        assertEquals(RefundDecisionType.FULL_REFUND, decision.type)
        assertEquals(RefundReasonCode.LEADER_CANCEL_BEFORE_48_HOURS, decision.reasonCode)
        assertTrue(decision.refundable)
    }

    @Test
    fun `leader cancel within 48 hours gets no refund`() {
        val decision = BookingRefundPolicy.evaluate(
            RefundPolicyContext(
                action = RefundPolicyAction.LEADER_CANCEL,
                bookingStatus = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                occurrenceStartsAtUtc = Instant.parse("2026-03-20T12:00:00Z"),
                evaluatedAtUtc = Instant.parse("2026-03-19T12:00:01Z")
            )
        )

        assertEquals(RefundDecisionType.NO_REFUND, decision.type)
        assertEquals(RefundReasonCode.LEADER_CANCEL_WITHIN_48_HOURS, decision.reasonCode)
        assertFalse(decision.refundable)
    }

    @Test
    fun `occurrence cancellation always gives full refund`() {
        val decision = BookingRefundPolicy.evaluate(
            RefundPolicyContext(
                action = RefundPolicyAction.OCCURRENCE_CANCEL,
                bookingStatus = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.AUTHORIZED,
                occurrenceStartsAtUtc = null,
                evaluatedAtUtc = Instant.parse("2026-03-16T12:00:00Z")
            )
        )

        assertEquals(RefundDecisionType.FULL_REFUND, decision.type)
        assertEquals(RefundReasonCode.OCCURRENCE_CANCELED, decision.reasonCode)
    }

    @Test
    fun `missing start time falls back to refund pending for leader cancel`() {
        val decision = BookingRefundPolicy.evaluate(
            RefundPolicyContext(
                action = RefundPolicyAction.LEADER_CANCEL,
                bookingStatus = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                occurrenceStartsAtUtc = null,
                evaluatedAtUtc = Instant.parse("2026-03-16T12:00:00Z")
            )
        )

        assertEquals(RefundDecisionType.REFUND_PENDING, decision.type)
        assertEquals(RefundReasonCode.OCCURRENCE_START_TIME_MISSING, decision.reasonCode)
        assertTrue(decision.refundable)
    }
}
