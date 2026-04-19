package com.demo.tourwave.application.payment

import com.demo.tourwave.adapter.out.persistence.booking.InMemoryBookingRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.payment.InMemoryPaymentProviderEventRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.payment.InMemoryPaymentReconciliationSummaryRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.payment.InMemoryPaymentRecordRepositoryAdapter
import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.payment.PaymentProviderEvent
import com.demo.tourwave.domain.payment.PaymentProviderEventStatus
import com.demo.tourwave.domain.payment.PaymentProviderEventType
import com.demo.tourwave.domain.payment.PaymentRecord
import com.demo.tourwave.domain.payment.PaymentRecordStatus
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReconciliationServiceTest {
    private val bookingRepository = InMemoryBookingRepositoryAdapter()
    private val paymentRecordRepository = InMemoryPaymentRecordRepositoryAdapter()
    private val paymentProviderEventRepository = InMemoryPaymentProviderEventRepositoryAdapter()
    private val summaryRepository = InMemoryPaymentReconciliationSummaryRepositoryAdapter()
    private val clock = Clock.fixed(Instant.parse("2026-03-18T12:00:00Z"), ZoneOffset.UTC)
    private val service =
        ReconciliationService(
            bookingRepository = bookingRepository,
            paymentRecordRepository = paymentRecordRepository,
            paymentProviderEventRepository = paymentProviderEventRepository,
            paymentReconciliationSummaryRepository = summaryRepository,
            clock = clock,
        )

    @Test
    fun `daily summary aggregates booking creation and payment record statuses`() {
        bookingRepository.save(
            Booking(
                occurrenceId = 10L,
                organizationId = 1L,
                leaderUserId = 101L,
                partySize = 2,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-17T02:00:00Z"),
            ),
        )
        bookingRepository.save(
            Booking(
                occurrenceId = 11L,
                organizationId = 1L,
                leaderUserId = 102L,
                partySize = 1,
                status = BookingStatus.REQUESTED,
                paymentStatus = PaymentStatus.AUTHORIZED,
                createdAt = Instant.parse("2026-03-17T05:00:00Z"),
            ),
        )
        paymentRecordRepository.save(
            PaymentRecord(
                bookingId = 1L,
                status = PaymentRecordStatus.CAPTURED,
                createdAtUtc = Instant.parse("2026-03-17T02:00:00Z"),
                updatedAtUtc = Instant.parse("2026-03-17T03:00:00Z"),
            ),
        )
        paymentRecordRepository.save(
            PaymentRecord(
                bookingId = 2L,
                status = PaymentRecordStatus.REFUND_FAILED_RETRYABLE,
                createdAtUtc = Instant.parse("2026-03-17T05:00:00Z"),
                updatedAtUtc = Instant.parse("2026-03-17T06:00:00Z"),
            ),
        )
        paymentProviderEventRepository.save(
            PaymentProviderEvent(
                providerName = "stub-pay",
                providerEventId = "evt-captured-1",
                eventType = PaymentProviderEventType.CAPTURED,
                bookingId = 1L,
                payloadJson = """{"providerEventId":"evt-captured-1"}""",
                signature = "sig",
                signatureKeyId = "current",
                payloadSha256 = "hash-1",
                status = PaymentProviderEventStatus.PROCESSED,
                receivedAtUtc = Instant.parse("2026-03-17T03:00:00Z"),
                processedAtUtc = Instant.parse("2026-03-17T03:00:01Z"),
            ),
        )
        paymentProviderEventRepository.save(
            PaymentProviderEvent(
                providerName = "stub-pay",
                providerEventId = "evt-refund-1",
                eventType = PaymentProviderEventType.REFUNDED,
                bookingId = 2L,
                payloadJson = """{"providerEventId":"evt-refund-1"}""",
                signature = "sig",
                signatureKeyId = "current",
                payloadSha256 = "hash-2",
                status = PaymentProviderEventStatus.PROCESSED,
                receivedAtUtc = Instant.parse("2026-03-17T06:30:00Z"),
                processedAtUtc = Instant.parse("2026-03-17T06:30:01Z"),
            ),
        )

        val summary = service.refreshDailySummary(LocalDate.parse("2026-03-17"))
        val csv = service.exportDailySummariesCsv(LocalDate.parse("2026-03-17"), LocalDate.parse("2026-03-17"))
        val mismatches = service.listMismatches(LocalDate.parse("2026-03-17"), LocalDate.parse("2026-03-17"))

        assertEquals(2, summary.bookingCreatedCount)
        assertEquals(1, summary.capturedCount)
        assertEquals(1, summary.providerCapturedCount)
        assertEquals(1, summary.providerRefundedCount)
        assertEquals(1, summary.refundFailedRetryableCount)
        assertEquals(1, summary.refundMismatchCount)
        assertEquals(1, mismatches.size)
        assertTrue(csv.contains("2026-03-17,2,0,1,1,1,0,0,0,1,0,0,1,0"))
    }
}
