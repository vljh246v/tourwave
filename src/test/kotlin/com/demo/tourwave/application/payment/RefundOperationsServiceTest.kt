package com.demo.tourwave.application.payment

import com.demo.tourwave.adapter.out.payment.InMemoryRefundExecutionAdapter
import com.demo.tourwave.adapter.out.persistence.audit.InMemoryAuditEventAdapter
import com.demo.tourwave.adapter.out.persistence.booking.InMemoryBookingRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.payment.InMemoryPaymentRecordRepositoryAdapter
import com.demo.tourwave.application.booking.RefundRetryService
import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.payment.PaymentRecord
import com.demo.tourwave.domain.payment.PaymentRecordStatus
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

class RefundOperationsServiceTest {
    private val bookingRepository = InMemoryBookingRepositoryAdapter()
    private val paymentRecordRepository = InMemoryPaymentRecordRepositoryAdapter()
    private val refundExecutionAdapter = InMemoryRefundExecutionAdapter()
    private val auditEventAdapter = InMemoryAuditEventAdapter()
    private val clock = Clock.fixed(Instant.parse("2026-03-18T12:00:00Z"), ZoneOffset.UTC)
    private val refundRetryService = RefundRetryService(
        paymentRecordRepository = paymentRecordRepository,
        bookingRepository = bookingRepository,
        refundExecutionPort = refundExecutionAdapter,
        auditEventPort = auditEventAdapter,
        maxRetryAttempts = 5,
        retryCooldown = Duration.ZERO,
        clock = clock
    )
    private val service = RefundOperationsService(
        paymentRecordRepository = paymentRecordRepository,
        bookingRepository = bookingRepository,
        refundRetryService = refundRetryService
    )

    @Test
    fun `refund ops queue exposes retryable items and manual remediation`() {
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 10L,
                organizationId = 1L,
                leaderUserId = 100L,
                partySize = 1,
                status = BookingStatus.CANCELED,
                paymentStatus = PaymentStatus.REFUND_PENDING,
                createdAt = Instant.parse("2026-03-17T00:00:00Z")
            )
        )
        paymentRecordRepository.save(
            PaymentRecord(
                bookingId = requireNotNull(booking.id),
                status = PaymentRecordStatus.REFUND_FAILED_RETRYABLE,
                lastRefundRequestId = "retry-ops-1",
                lastRefundReasonCode = "BOOKING_REJECTED",
                lastErrorCode = "TEMPORARY",
                refundRetryCount = 1,
                createdAtUtc = Instant.parse("2026-03-17T00:00:00Z"),
                updatedAtUtc = Instant.parse("2026-03-17T01:00:00Z")
            )
        )

        val queued = service.listRefundOpsQueue()
        val retried = service.retryBookingRefund(requireNotNull(booking.id))

        assertEquals(1, queued.size)
        assertEquals(PaymentRecordStatus.REFUNDED, retried.recordStatus)
        assertEquals(PaymentStatus.REFUNDED, bookingRepository.findById(requireNotNull(booking.id))?.paymentStatus)
    }
}
