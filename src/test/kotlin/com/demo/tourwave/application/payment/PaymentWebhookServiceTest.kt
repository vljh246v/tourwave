package com.demo.tourwave.application.payment

import com.demo.tourwave.adapter.out.payment.InMemoryRefundExecutionAdapter
import com.demo.tourwave.adapter.out.persistence.booking.InMemoryBookingRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.payment.InMemoryPaymentProviderEventRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.payment.InMemoryPaymentRecordRepositoryAdapter
import com.demo.tourwave.application.booking.PaymentLedgerService
import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.occurrence.Occurrence
import com.demo.tourwave.domain.payment.PaymentProviderEventStatus
import com.demo.tourwave.domain.payment.PaymentProviderEventType
import com.demo.tourwave.domain.payment.PaymentRecordStatus
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PaymentWebhookServiceTest {
    private val bookingRepository = InMemoryBookingRepositoryAdapter()
    private val paymentRecordRepository = InMemoryPaymentRecordRepositoryAdapter()
    private val paymentProviderEventRepository = InMemoryPaymentProviderEventRepositoryAdapter()
    private val providerAdapter = InMemoryRefundExecutionAdapter()
    private val clock = Clock.fixed(Instant.parse("2026-03-18T12:00:00Z"), ZoneOffset.UTC)
    private val paymentLedgerService = PaymentLedgerService(
        paymentRecordRepository = paymentRecordRepository,
        paymentProviderPort = providerAdapter,
        refundExecutionPort = providerAdapter,
        clock = clock
    )
    private val service = PaymentWebhookService(
        paymentProviderEventRepository = paymentProviderEventRepository,
        bookingRepository = bookingRepository,
        paymentLedgerService = paymentLedgerService,
        webhookSecret = "webhook-secret",
        clock = clock
    )

    @Test
    fun `webhook is replay safe and updates payment state`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 10L,
                organizationId = 1L,
                leaderUserId = 100L,
                partySize = 2,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.AUTHORIZED,
                createdAt = Instant.parse("2026-03-17T00:00:00Z")
            )
        )
        paymentLedgerService.initialize(
            booking = booking,
            occurrence = Occurrence(id = 10L, organizationId = 1L, capacity = 10, unitPrice = 40000, currency = "KRW"),
            actorUserId = 100L
        )

        val body = """{"providerName":"stub-pay","providerEventId":"evt-1","eventType":"CAPTURED","bookingId":${booking.id},"providerCaptureId":"cap-evt-1","providerReference":"capture-ref-1","retryable":true}"""
        val signature = service.expectedSignature(body)

        val first = service.receive(
            PaymentWebhookCommand(
                providerName = "stub-pay",
                providerEventId = "evt-1",
                eventType = PaymentProviderEventType.CAPTURED,
                bookingId = requireNotNull(booking.id),
                providerCaptureId = "cap-evt-1",
                providerReference = "capture-ref-1",
                rawPayload = body,
                signature = signature
            )
        )
        val duplicate = service.receive(
            PaymentWebhookCommand(
                providerName = "stub-pay",
                providerEventId = "evt-1",
                eventType = PaymentProviderEventType.CAPTURED,
                bookingId = requireNotNull(booking.id),
                providerCaptureId = "cap-evt-1",
                providerReference = "capture-ref-1",
                rawPayload = body,
                signature = signature
            )
        )

        assertEquals(false, first.duplicate)
        assertEquals(PaymentProviderEventStatus.PROCESSED, first.eventStatus)
        assertEquals(PaymentRecordStatus.CAPTURED, first.paymentRecordStatus)
        assertEquals(true, duplicate.duplicate)
        assertEquals(PaymentStatus.PAID, bookingRepository.findById(requireNotNull(booking.id))?.paymentStatus)
        assertEquals(PaymentRecordStatus.CAPTURED, paymentRecordRepository.findByBookingId(requireNotNull(booking.id))?.status)
    }

    @Test
    fun `webhook rejects invalid signature and persists rejection`() {
        val body = """{"providerName":"stub-pay","providerEventId":"evt-2","eventType":"REFUNDED","bookingId":999}"""

        assertFailsWith<IllegalArgumentException> {
            service.receive(
                PaymentWebhookCommand(
                    providerName = "stub-pay",
                    providerEventId = "evt-2",
                    eventType = PaymentProviderEventType.REFUNDED,
                    bookingId = 999L,
                    rawPayload = body,
                    signature = "invalid"
                )
            )
        }

        assertEquals(
            PaymentProviderEventStatus.REJECTED_SIGNATURE,
            paymentProviderEventRepository.findByProviderEventId("evt-2")?.status
        )
    }
}
