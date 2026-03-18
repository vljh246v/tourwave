package com.demo.tourwave.application.booking

import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.common.port.ActorAuthContext
import com.demo.tourwave.application.participant.ParticipantAccessPolicy
import com.demo.tourwave.application.participant.port.BookingParticipantRepository
import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.booking.RefundDecisionType
import com.demo.tourwave.domain.booking.RefundReasonCode
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.occurrence.Occurrence
import com.demo.tourwave.domain.participant.BookingParticipant
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

class BookingRefundPreviewServiceTest {
    private val bookingRepository = mock(BookingRepository::class.java)
    private val occurrenceRepository = mock(OccurrenceRepository::class.java)
    private val participantRepository = mock(BookingParticipantRepository::class.java)
    private val paymentProviderAdapter = com.demo.tourwave.adapter.out.payment.InMemoryRefundExecutionAdapter()
    private val paymentLedgerService = PaymentLedgerService(
        paymentRecordRepository = com.demo.tourwave.adapter.out.persistence.payment.InMemoryPaymentRecordRepositoryAdapter(),
        paymentProviderPort = paymentProviderAdapter,
        refundExecutionPort = paymentProviderAdapter,
        clock = Clock.fixed(Instant.parse("2026-03-16T12:00:00Z"), ZoneOffset.UTC)
    )
    private val participantAccessPolicy = ParticipantAccessPolicy(bookingRepository, participantRepository)
    private val clock = Clock.fixed(Instant.parse("2026-03-16T12:00:00Z"), ZoneOffset.UTC)
    private val service = BookingRefundPreviewService(
        bookingRepository = bookingRepository,
        occurrenceRepository = occurrenceRepository,
        participantAccessPolicy = participantAccessPolicy,
        paymentLedgerService = paymentLedgerService,
        clock = clock
    )

    @Test
    fun `leader can preview full refund before deadline`() {
        val booking = booking()
        whenever(bookingRepository.findById(11L)).thenReturn(booking)
        whenever(occurrenceRepository.getOrCreate(701L)).thenReturn(
            Occurrence(
                id = 701L,
                organizationId = 31L,
                capacity = 10,
                startsAtUtc = Instant.parse("2026-03-20T12:00:00Z")
            )
        )
        whenever(participantRepository.findByBookingIdAndUserId(11L, 501L)).thenReturn(
            BookingParticipant.leader(bookingId = 11L, userId = 501L, createdAt = booking.createdAt).copy(id = 1L)
        )

        val result = service.getPreview(
            GetBookingRefundPreviewQuery(
                bookingId = 11L,
                actor = ActorAuthContext(actorUserId = 501L)
            )
        )

        assertEquals(RefundDecisionType.FULL_REFUND, result.refundDecisionType)
        assertEquals(RefundReasonCode.LEADER_CANCEL_BEFORE_48_HOURS, result.refundReasonCode)
    }

    @Test
    fun `non leader participant cannot preview cancellation refund`() {
        val booking = booking()
        whenever(bookingRepository.findById(11L)).thenReturn(booking)
        whenever(participantRepository.findByBookingIdAndUserId(11L, 777L)).thenReturn(
            BookingParticipant(
                id = 3L,
                bookingId = 11L,
                userId = 777L,
                status = com.demo.tourwave.domain.participant.BookingParticipantStatus.ACCEPTED,
                invitedAt = booking.createdAt,
                respondedAt = booking.createdAt,
                createdAt = booking.createdAt
            )
        )

        val exception = assertThrows<DomainException> {
            service.getPreview(
                GetBookingRefundPreviewQuery(
                    bookingId = 11L,
                    actor = ActorAuthContext(actorUserId = 777L)
                )
            )
        }

        assertEquals(403, exception.status)
    }

    private fun booking(): Booking {
        return Booking(
            id = 11L,
            occurrenceId = 701L,
            organizationId = 31L,
            leaderUserId = 501L,
            partySize = 2,
            status = BookingStatus.CONFIRMED,
            paymentStatus = PaymentStatus.PAID,
            createdAt = Instant.parse("2026-03-10T00:00:00Z")
        )
    }
}
