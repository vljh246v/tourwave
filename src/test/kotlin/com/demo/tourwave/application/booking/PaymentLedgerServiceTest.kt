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
    private val paymentProviderAdapter = InMemoryRefundExecutionAdapter()
    private val clock = Clock.fixed(Instant.parse("2026-03-16T12:00:00Z"), ZoneOffset.UTC)
    private val service = PaymentLedgerService(paymentRecordRepository, paymentProviderAdapter, paymentProviderAdapter, clock)

    @Test
    fun `capture updates payment record and booking status`() {
        val booking = booking(31L, BookingStatus.REQUESTED, PaymentStatus.AUTHORIZED)
        val occurrence = occurrence(801L)
        service.initialize(booking, occurrence, actorUserId = 900L)

        val captured = service.capture(booking, actorUserId = 900L)

        assertEquals(PaymentStatus.PAID, captured.paymentStatus)
        val record = paymentRecordRepository.findByBookingId(31L)
        assertEquals(PaymentRecordStatus.CAPTURED, record?.status)
        assertEquals("pay-31", record?.providerPaymentKey)
        assertEquals("cap-31", record?.providerCaptureId)
    }

    @Test
    fun `retryable refund failure leaves booking in refund pending`() {
        val booking = booking(32L, BookingStatus.CANCELED, PaymentStatus.PAID)
        service.initialize(booking, occurrence(802L), actorUserId = 900L)
        paymentProviderAdapter.scriptRetryableFailure(32L, "gateway-timeout")

        val refunded = service.applyRefundPolicy(
            booking = booking,
            occurrence = occurrence(803L),
            action = RefundPolicyAction.OCCURRENCE_CANCEL,
            actorUserId = 900L
        )

        assertEquals(PaymentStatus.REFUND_PENDING, refunded.paymentStatus)
        val record = paymentRecordRepository.findByBookingId(32L)
        assertEquals(PaymentRecordStatus.REFUND_FAILED_RETRYABLE, record?.status)
        assertEquals("gateway-timeout", record?.lastErrorCode)
        assertEquals(1, record?.refundRetryCount)
    }

    @Test
    fun `within 48 hours leader cancellation stores no refund state`() {
        val booking = booking(33L, BookingStatus.CANCELED, PaymentStatus.PAID)
        service.initialize(booking, occurrence(804L), actorUserId = 900L)

        val result = service.applyRefundPolicy(
            booking = booking,
            occurrence = occurrence(805L, startsAtUtc = Instant.parse("2026-03-17T00:00:00Z")),
            action = RefundPolicyAction.LEADER_CANCEL,
            actorUserId = 900L
        )

        assertEquals(PaymentStatus.PAID, result.paymentStatus)
        assertEquals(PaymentRecordStatus.NO_REFUND, paymentRecordRepository.findByBookingId(33L)?.status)
    }

    @Test
    fun `manual review refund failure stores operator review state`() {
        val booking = booking(34L, BookingStatus.CANCELED, PaymentStatus.PAID)
        service.initialize(booking, occurrence(806L), actorUserId = 900L)
        paymentProviderAdapter.scriptReviewRequired(34L, "gateway-manual-review")

        val refunded = service.applyRefundPolicy(
            booking = booking,
            occurrence = occurrence(807L),
            action = RefundPolicyAction.OCCURRENCE_CANCEL,
            actorUserId = 900L
        )

        assertEquals(PaymentStatus.REFUND_PENDING, refunded.paymentStatus)
        assertEquals(PaymentRecordStatus.REFUND_REVIEW_REQUIRED, paymentRecordRepository.findByBookingId(34L)?.status)
        assertEquals("gateway-manual-review", paymentRecordRepository.findByBookingId(34L)?.lastErrorCode)
    }

    @Test
    fun `webhook status update stores provider metadata`() {
        val booking = booking(35L, BookingStatus.CONFIRMED, PaymentStatus.AUTHORIZED)
        service.initialize(booking, occurrence(808L), actorUserId = 900L)

        val record = service.applyWebhookStatus(
            bookingId = 35L,
            status = PaymentRecordStatus.CAPTURED,
            providerName = "stub-pay",
            providerReference = "capture-webhook-35",
            providerCaptureId = "cap-webhook-35",
            webhookEventId = "evt-35"
        )

        assertEquals("cap-webhook-35", record.providerCaptureId)
        assertEquals("capture-webhook-35", record.lastProviderReference)
        assertEquals("evt-35", record.lastWebhookEventId)
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

    private fun occurrence(id: Long, startsAtUtc: Instant = Instant.parse("2026-03-20T12:00:00Z")): Occurrence {
        return Occurrence(
            id = id,
            organizationId = 31L,
            capacity = 10,
            startsAtUtc = startsAtUtc,
            unitPrice = 50000,
            currency = "KRW"
        )
    }
}
