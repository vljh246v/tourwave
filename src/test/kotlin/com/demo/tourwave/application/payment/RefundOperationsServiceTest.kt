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
import com.demo.tourwave.domain.payment.RefundRemediationAction
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
    private val refundRetryService =
        RefundRetryService(
            paymentRecordRepository = paymentRecordRepository,
            bookingRepository = bookingRepository,
            refundExecutionPort = refundExecutionAdapter,
            auditEventPort = auditEventAdapter,
            maxRetryAttempts = 5,
            retryCooldown = Duration.ZERO,
            clock = clock,
        )
    private val service =
        RefundOperationsService(
            paymentRecordRepository = paymentRecordRepository,
            bookingRepository = bookingRepository,
            refundRetryService = refundRetryService,
            auditEventPort = auditEventAdapter,
            maxRetryAttempts = 5,
            retryCooldown = Duration.ZERO,
            clock = clock,
        )

    @Test
    fun `refund ops queue exposes retryable items and manual remediation`() {
        val booking =
            bookingRepository.save(
                Booking(
                    occurrenceId = 10L,
                    organizationId = 1L,
                    leaderUserId = 100L,
                    partySize = 1,
                    status = BookingStatus.CANCELED,
                    paymentStatus = PaymentStatus.REFUND_PENDING,
                    createdAt = Instant.parse("2026-03-17T00:00:00Z"),
                ),
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
                updatedAtUtc = Instant.parse("2026-03-17T01:00:00Z"),
            ),
        )

        val queued = service.listRefundOpsQueue()
        val retried =
            service.remediateBookingRefund(
                requireNotNull(booking.id),
                RefundRemediationCommand(actorUserId = 999L, reasonCode = "MANUAL_RETRY", note = "operator retry"),
            )

        assertEquals(1, queued.size)
        assertEquals(false, queued.first().reviewRequired)
        assertEquals(PaymentRecordStatus.REFUNDED, retried.recordStatus)
        assertEquals(PaymentStatus.REFUNDED, bookingRepository.findById(requireNotNull(booking.id))?.paymentStatus)
        assertEquals(RefundRemediationAction.RETRY, paymentRecordRepository.findByBookingId(requireNotNull(booking.id))?.lastRemediationAction)
        assertEquals("REFUND_REMEDIATION_RETRY", auditEventAdapter.all().last().action)
    }

    @Test
    fun `manual mark review required is explicit in queue`() {
        val booking =
            bookingRepository.save(
                Booking(
                    occurrenceId = 11L,
                    organizationId = 1L,
                    leaderUserId = 100L,
                    partySize = 1,
                    status = BookingStatus.CANCELED,
                    paymentStatus = PaymentStatus.REFUND_PENDING,
                    createdAt = Instant.parse("2026-03-17T00:00:00Z"),
                ),
            )
        paymentRecordRepository.save(
            PaymentRecord(
                bookingId = requireNotNull(booking.id),
                status = PaymentRecordStatus.REFUND_FAILED_RETRYABLE,
                refundRetryCount = 4,
                lastRefundAttemptedAtUtc = Instant.parse("2026-03-18T11:00:00Z"),
                createdAtUtc = Instant.parse("2026-03-17T00:00:00Z"),
                updatedAtUtc = Instant.parse("2026-03-18T11:00:00Z"),
            ),
        )

        val updated =
            service.remediateBookingRefund(
                requireNotNull(booking.id),
                RefundRemediationCommand(
                    actorUserId = 1001L,
                    action = RefundRemediationAction.MARK_REVIEW_REQUIRED,
                    reasonCode = "MANUAL_REVIEW",
                    note = "needs finance review",
                ),
            )

        assertEquals(true, updated.reviewRequired)
        assertEquals(PaymentRecordStatus.REFUND_REVIEW_REQUIRED, updated.recordStatus)
        assertEquals(RefundRemediationAction.MARK_REVIEW_REQUIRED, updated.lastRemediationAction)
        assertEquals("REFUND_REMEDIATION_MARK_REVIEW_REQUIRED", auditEventAdapter.all().last().action)
    }
}
