package com.demo.tourwave.application.booking

import com.demo.tourwave.adapter.out.payment.InMemoryRefundExecutionAdapter
import com.demo.tourwave.adapter.out.persistence.payment.InMemoryPaymentRecordRepositoryAdapter
import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.booking.RefundPolicyAction
import com.demo.tourwave.domain.occurrence.Occurrence
import com.demo.tourwave.domain.payment.PaymentRecordStatus
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

class PaymentLedgerServiceTest {
    private val paymentRecordRepository = InMemoryPaymentRecordRepositoryAdapter()
    private val refundExecutionAdapter = InMemoryRefundExecutionAdapter()
    private val clock = Clock.fixed(Instant.parse("2026-03-16T12:00:00Z"), ZoneOffset.UTC)
    private val service = PaymentLedgerService(paymentRecordRepository, refundExecutionAdapter, clock)

    @Test
    fun `capture updates payment record and booking status`() {
        val booking = booking(31L, BookingStatus.REQUESTED, PaymentStatus.AUTHORIZED)
        service.initialize(booking)

        val captured = service.capture(booking)

        assertEquals(PaymentStatus.PAID, captured.paymentStatus)
        assertEquals(PaymentRecordStatus.CAPTURED, paymentRecordRepository.findByBookingId(31L)?.status)
    }

    @Test
    fun `retryable refund failure leaves booking in refund pending`() {
        val booking = booking(32L, BookingStatus.CANCELED, PaymentStatus.PAID)
        service.initialize(booking)
        refundExecutionAdapter.scriptRetryableFailure(32L, "gateway-timeout")

        val refunded = service.applyRefundPolicy(
            booking = booking,
            occurrence = Occurrence(
                id = 801L,
                organizationId = 31L,
                capacity = 10,
                startsAtUtc = Instant.parse("2026-03-20T12:00:00Z")
            ),
            action = RefundPolicyAction.OCCURRENCE_CANCEL,
            actorUserId = 900L
        )

        assertEquals(PaymentStatus.REFUND_PENDING, refunded.paymentStatus)
        assertEquals(PaymentRecordStatus.REFUND_FAILED_RETRYABLE, paymentRecordRepository.findByBookingId(32L)?.status)
        assertEquals("gateway-timeout", paymentRecordRepository.findByBookingId(32L)?.lastErrorCode)
    }

    @Test
    fun `within 48 hours leader cancellation stores no refund state`() {
        val booking = booking(33L, BookingStatus.CANCELED, PaymentStatus.PAID)
        service.initialize(booking)

        val result = service.applyRefundPolicy(
            booking = booking,
            occurrence = Occurrence(
                id = 802L,
                organizationId = 31L,
                capacity = 10,
                startsAtUtc = Instant.parse("2026-03-17T00:00:00Z")
            ),
            action = RefundPolicyAction.LEADER_CANCEL,
            actorUserId = 900L
        )

        assertEquals(PaymentStatus.PAID, result.paymentStatus)
        assertEquals(PaymentRecordStatus.NO_REFUND, paymentRecordRepository.findByBookingId(33L)?.status)
    }

    @Test
    fun `manual review refund failure stores operator review state`() {
        val booking = booking(34L, BookingStatus.CANCELED, PaymentStatus.PAID)
        service.initialize(booking)
        refundExecutionAdapter.scriptReviewRequired(34L, "gateway-manual-review")

        val refunded = service.applyRefundPolicy(
            booking = booking,
            occurrence = Occurrence(
                id = 803L,
                organizationId = 31L,
                capacity = 10,
                startsAtUtc = Instant.parse("2026-03-20T12:00:00Z")
            ),
            action = RefundPolicyAction.OCCURRENCE_CANCEL,
            actorUserId = 900L
        )

        assertEquals(PaymentStatus.REFUND_PENDING, refunded.paymentStatus)
        assertEquals(PaymentRecordStatus.REFUND_REVIEW_REQUIRED, paymentRecordRepository.findByBookingId(34L)?.status)
        assertEquals("gateway-manual-review", paymentRecordRepository.findByBookingId(34L)?.lastErrorCode)
    }

    private fun booking(id: Long, status: BookingStatus, paymentStatus: PaymentStatus): Booking {
        return Booking(
            id = id,
            occurrenceId = 701L,
            organizationId = 31L,
            leaderUserId = 501L,
            partySize = 2,
            status = status,
            paymentStatus = paymentStatus,
            createdAt = Instant.parse("2026-03-10T00:00:00Z")
        )
    }
}
