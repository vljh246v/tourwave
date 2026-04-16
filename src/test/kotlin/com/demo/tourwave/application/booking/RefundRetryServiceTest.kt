package com.demo.tourwave.application.booking

import com.demo.tourwave.adapter.out.payment.InMemoryRefundExecutionAdapter
import com.demo.tourwave.adapter.out.persistence.audit.InMemoryAuditEventAdapter
import com.demo.tourwave.adapter.out.persistence.booking.InMemoryBookingRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.payment.InMemoryPaymentRecordRepositoryAdapter
import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.payment.PaymentRecord
import com.demo.tourwave.domain.payment.PaymentRecordStatus
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

class RefundRetryServiceTest {
    private val bookingRepository = InMemoryBookingRepositoryAdapter()
    private val paymentRecordRepository = InMemoryPaymentRecordRepositoryAdapter()
    private val refundExecutionAdapter = InMemoryRefundExecutionAdapter()
    private val auditEventAdapter = InMemoryAuditEventAdapter()
    private val clock = Clock.fixed(Instant.parse("2026-03-17T00:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `retryable refund success settles booking and payment record`() {
        val booking =
            bookingRepository.save(
                Booking(
                    occurrenceId = 1L,
                    organizationId = 1L,
                    leaderUserId = 10L,
                    partySize = 1,
                    status = BookingStatus.CANCELED,
                    paymentStatus = PaymentStatus.REFUND_PENDING,
                    createdAt = Instant.parse("2026-03-10T00:00:00Z"),
                ),
            )
        paymentRecordRepository.save(
            PaymentRecord(
                bookingId = requireNotNull(booking.id),
                status = PaymentRecordStatus.REFUND_FAILED_RETRYABLE,
                lastRefundRequestId = "retry-1",
                lastRefundReasonCode = "BOOKING_REJECTED",
                lastErrorCode = "TEMPORARY",
                createdAtUtc = Instant.parse("2026-03-10T00:00:00Z"),
                updatedAtUtc = Instant.parse("2026-03-10T00:01:00Z"),
            ),
        )

        val service =
            RefundRetryService(
                paymentRecordRepository = paymentRecordRepository,
                bookingRepository = bookingRepository,
                refundExecutionPort = refundExecutionAdapter,
                auditEventPort = auditEventAdapter,
                maxRetryAttempts = 5,
                retryCooldown = java.time.Duration.ZERO,
                clock = clock,
            )

        val result = service.retryPendingRefunds()

        assertEquals(1, result.scannedCount)
        assertEquals(1, result.eligibleCount)
        assertEquals(1, result.refundedCount)
        assertEquals(PaymentRecordStatus.REFUNDED, paymentRecordRepository.findByBookingId(requireNotNull(booking.id))?.status)
        assertEquals(PaymentStatus.REFUNDED, bookingRepository.findById(requireNotNull(booking.id))?.paymentStatus)
    }

    @Test
    fun `retryable refund can escalate to review required`() {
        val booking =
            bookingRepository.save(
                Booking(
                    occurrenceId = 2L,
                    organizationId = 1L,
                    leaderUserId = 11L,
                    partySize = 1,
                    status = BookingStatus.CANCELED,
                    paymentStatus = PaymentStatus.REFUND_PENDING,
                    createdAt = Instant.parse("2026-03-10T00:00:00Z"),
                ),
            )
        paymentRecordRepository.save(
            PaymentRecord(
                bookingId = requireNotNull(booking.id),
                status = PaymentRecordStatus.REFUND_FAILED_RETRYABLE,
                lastRefundRequestId = "retry-2",
                lastRefundReasonCode = "BOOKING_REJECTED",
                lastErrorCode = "TEMPORARY",
                createdAtUtc = Instant.parse("2026-03-10T00:00:00Z"),
                updatedAtUtc = Instant.parse("2026-03-10T00:01:00Z"),
            ),
        )
        refundExecutionAdapter.scriptReviewRequired(requireNotNull(booking.id), "MANUAL_CHECK")

        val service =
            RefundRetryService(
                paymentRecordRepository = paymentRecordRepository,
                bookingRepository = bookingRepository,
                refundExecutionPort = refundExecutionAdapter,
                auditEventPort = auditEventAdapter,
                maxRetryAttempts = 5,
                retryCooldown = java.time.Duration.ZERO,
                clock = clock,
            )

        val result = service.retryPendingRefunds()

        assertEquals(1, result.reviewRequiredCount)
        assertEquals(
            PaymentRecordStatus.REFUND_REVIEW_REQUIRED,
            paymentRecordRepository.findByBookingId(requireNotNull(booking.id))?.status,
        )
        assertEquals(PaymentStatus.REFUND_PENDING, bookingRepository.findById(requireNotNull(booking.id))?.paymentStatus)
    }

    @Test
    fun `manual retry escalates to review required after max attempts`() {
        val booking =
            bookingRepository.save(
                Booking(
                    occurrenceId = 3L,
                    organizationId = 1L,
                    leaderUserId = 12L,
                    partySize = 1,
                    status = BookingStatus.CANCELED,
                    paymentStatus = PaymentStatus.REFUND_PENDING,
                    createdAt = Instant.parse("2026-03-10T00:00:00Z"),
                ),
            )
        paymentRecordRepository.save(
            PaymentRecord(
                bookingId = requireNotNull(booking.id),
                status = PaymentRecordStatus.REFUND_FAILED_RETRYABLE,
                lastRefundRequestId = "retry-3",
                lastRefundReasonCode = "BOOKING_REJECTED",
                lastErrorCode = "TEMPORARY",
                refundRetryCount = 1,
                createdAtUtc = Instant.parse("2026-03-10T00:00:00Z"),
                updatedAtUtc = Instant.parse("2026-03-10T00:01:00Z"),
            ),
        )
        refundExecutionAdapter.scriptRetryableFailure(requireNotNull(booking.id), "TEMPORARY")

        val service =
            RefundRetryService(
                paymentRecordRepository = paymentRecordRepository,
                bookingRepository = bookingRepository,
                refundExecutionPort = refundExecutionAdapter,
                auditEventPort = auditEventAdapter,
                maxRetryAttempts = 2,
                retryCooldown = java.time.Duration.ZERO,
                clock = clock,
            )

        val status = service.retryBookingRefund(requireNotNull(booking.id))

        assertEquals(PaymentRecordStatus.REFUND_REVIEW_REQUIRED, status)
        assertEquals(
            PaymentRecordStatus.REFUND_REVIEW_REQUIRED,
            paymentRecordRepository.findByBookingId(requireNotNull(booking.id))?.status,
        )
    }
}
