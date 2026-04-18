package com.demo.tourwave.application.booking

import com.demo.tourwave.adapter.out.payment.InMemoryRefundExecutionAdapter
import com.demo.tourwave.adapter.out.persistence.audit.InMemoryAuditEventAdapter
import com.demo.tourwave.adapter.out.persistence.booking.InMemoryBookingRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.occurrence.InMemoryOccurrenceRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.payment.InMemoryPaymentRecordRepositoryAdapter
import com.demo.tourwave.application.common.TimeWindowPolicyService
import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.occurrence.Occurrence
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OfferExpirationServiceTest {
    private val bookingRepository = InMemoryBookingRepositoryAdapter()
    private val occurrenceRepository = InMemoryOccurrenceRepositoryAdapter()
    private val auditEventAdapter = InMemoryAuditEventAdapter()
    private val paymentRecordRepository = InMemoryPaymentRecordRepositoryAdapter()
    private val refundExecutionAdapter = InMemoryRefundExecutionAdapter()
    private val clock = Clock.fixed(Instant.parse("2026-03-16T12:00:00Z"), ZoneOffset.UTC)
    private val paymentLedgerService = PaymentLedgerService(paymentRecordRepository, refundExecutionAdapter, refundExecutionAdapter, clock)
    private val service =
        OfferExpirationService(
            bookingRepository = bookingRepository,
            occurrenceRepository = occurrenceRepository,
            auditEventPort = auditEventAdapter,
            paymentLedgerService = paymentLedgerService,
            timeWindowPolicyService = TimeWindowPolicyService(),
            clock = clock,
        )

    @Test
    fun `expire offers settles refund and promotes next waitlist booking`() {
        occurrenceRepository.save(
            Occurrence(
                id = 4001L,
                organizationId = 31L,
                capacity = 2,
                startsAtUtc = Instant.parse("2026-03-20T12:00:00Z"),
            ),
        )
        val expired =
            bookingRepository.save(
                Booking(
                    occurrenceId = 4001L,
                    organizationId = 31L,
                    leaderUserId = 101L,
                    partySize = 2,
                    status = BookingStatus.OFFERED,
                    paymentStatus = PaymentStatus.AUTHORIZED,
                    offerExpiresAtUtc = Instant.parse("2026-03-16T11:00:00Z"),
                    createdAt = Instant.parse("2026-03-10T00:00:00Z"),
                ),
            )
        paymentLedgerService.initialize(
            booking = expired,
            occurrence = occurrenceRepository.getOrCreate(4001L),
            actorUserId = 101L,
        )
        val waiting =
            bookingRepository.save(
                Booking(
                    occurrenceId = 4001L,
                    organizationId = 31L,
                    leaderUserId = 102L,
                    partySize = 2,
                    status = BookingStatus.WAITLISTED,
                    paymentStatus = PaymentStatus.AUTHORIZED,
                    createdAt = Instant.parse("2026-03-10T00:01:00Z"),
                ),
            )

        val result = service.expireOffers()

        val expiredAfter = bookingRepository.findById(requireNotNull(expired.id))!!
        val waitingAfter = bookingRepository.findById(requireNotNull(waiting.id))!!
        assertEquals(listOf(requireNotNull(expired.id)), result.expiredBookingIds)
        assertEquals(BookingStatus.EXPIRED, expiredAfter.status)
        assertEquals(PaymentStatus.REFUNDED, expiredAfter.paymentStatus)
        assertEquals(BookingStatus.OFFERED, waitingAfter.status)
        assertTrue(auditEventAdapter.all().any { it.action == "OFFER_EXPIRED" && it.actorType.name == "JOB" && it.reasonCode == "OFFER_TIMEOUT" })
        assertTrue(auditEventAdapter.all().any { it.action == "WAITLIST_PROMOTED_TO_OFFER" && it.actorType.name == "JOB" })
    }
}
