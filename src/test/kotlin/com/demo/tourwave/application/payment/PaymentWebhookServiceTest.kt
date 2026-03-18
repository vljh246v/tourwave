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
        webhookSecrets = listOf("current:webhook-secret", "previous:previous-secret"),
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
        val signature = service.expectedSignature(body, "current")

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

        assertFailsWith<com.demo.tourwave.domain.common.DomainException> {
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

    @Test
    fun `webhook accepts rotated secret header format and records key id`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 11L,
                organizationId = 1L,
                leaderUserId = 101L,
                partySize = 1,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.AUTHORIZED,
                createdAt = Instant.parse("2026-03-17T00:00:00Z")
            )
        )
        paymentLedgerService.initialize(
            booking = booking,
            occurrence = Occurrence(id = 11L, organizationId = 1L, capacity = 10, unitPrice = 40000, currency = "KRW"),
            actorUserId = 101L
        )
        val body = """{"providerName":"stub-pay","providerEventId":"evt-rotation","eventType":"CAPTURED","bookingId":${booking.id},"providerCaptureId":"cap-rotation","providerReference":"capture-ref-rotation","retryable":true}"""

        service.receive(
            PaymentWebhookCommand(
                providerName = "stub-pay",
                providerEventId = "evt-rotation",
                eventType = PaymentProviderEventType.CAPTURED,
                bookingId = requireNotNull(booking.id),
                providerCaptureId = "cap-rotation",
                providerReference = "capture-ref-rotation",
                rawPayload = body,
                signature = "current:${service.expectedSignature(body, "current")}"
            )
        )

        assertEquals("current", paymentProviderEventRepository.findByProviderEventId("evt-rotation")?.signatureKeyId)
    }

    @Test
    fun `record malformed payload persists malformed status when signature is valid`() {
        val rawBody = """{"providerName":"stub-pay","providerEventId":"""

        service.recordMalformedPayload(rawBody, "kid=current,signature=${service.expectedSignature(rawBody, "current")}", "unexpected EOF")

        val event = paymentProviderEventRepository.findReceivedBetween(
            Instant.parse("2026-03-18T00:00:00Z"),
            Instant.parse("2026-03-19T00:00:00Z")
        ).single()
        assertEquals(PaymentProviderEventStatus.MALFORMED_PAYLOAD, event.status)
        assertEquals("current", event.signatureKeyId)
    }
}
