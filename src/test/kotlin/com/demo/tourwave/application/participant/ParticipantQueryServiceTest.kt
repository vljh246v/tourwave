package com.demo.tourwave.application.participant

import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.common.TimeWindowPolicyService
import com.demo.tourwave.application.common.port.ActorAuthContext
import com.demo.tourwave.application.participant.port.BookingParticipantRepository
import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.occurrence.Occurrence
import com.demo.tourwave.domain.participant.BookingParticipant
import com.demo.tourwave.domain.participant.BookingParticipantStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

class ParticipantQueryServiceTest {
    private val bookingRepository = mock(BookingRepository::class.java)
    private val occurrenceRepository = mock(OccurrenceRepository::class.java)
    private val bookingParticipantRepository = mock(BookingParticipantRepository::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-03-16T12:00:00Z"), ZoneOffset.UTC)
    private val timeWindowPolicyService =
        TimeWindowPolicyService(
            invitationWindowMinutes = 360,
            invitationExpiryHours = 48,
            refundFullWindowHours = 48,
        )

    private val lifecycleService =
        ParticipantInvitationLifecycleService(
            bookingRepository = bookingRepository,
            occurrenceRepository = occurrenceRepository,
            bookingParticipantRepository = bookingParticipantRepository,
            timeWindowPolicyService = timeWindowPolicyService,
            clock = clock,
        )

    private val accessPolicy =
        ParticipantAccessPolicy(
            bookingRepository = bookingRepository,
            bookingParticipantRepository = bookingParticipantRepository,
        )

    private val queryService =
        ParticipantQueryService(
            participantAccessPolicy = accessPolicy,
            participantInvitationLifecycleService = lifecycleService,
        )

    @Test
    fun `booking participant can list participants and expired invitation is refreshed`() {
        val booking =
            Booking(
                id = 91L,
                occurrenceId = 501L,
                organizationId = 31L,
                leaderUserId = 101L,
                partySize = 3,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-10T00:00:00Z"),
            )
        val leader =
            BookingParticipant.leader(
                bookingId = 91L,
                userId = 101L,
                createdAt = Instant.parse("2026-03-10T00:00:00Z"),
            ).copy(id = 1L)
        val expiredInvite =
            BookingParticipant(
                id = 2L,
                bookingId = 91L,
                userId = 202L,
                status = BookingParticipantStatus.INVITED,
                invitedAt = Instant.parse("2026-03-14T11:00:00Z"),
                createdAt = Instant.parse("2026-03-14T11:00:00Z"),
            )

        whenever(bookingRepository.findById(91L)).thenReturn(booking)
        whenever(occurrenceRepository.getOrCreate(501L)).thenReturn(
            Occurrence(id = 501L, organizationId = 31L, capacity = 10),
        )
        whenever(bookingParticipantRepository.findByBookingIdAndUserId(91L, 101L)).thenReturn(leader)
        whenever(bookingParticipantRepository.findByBookingId(91L)).thenReturn(listOf(leader, expiredInvite))
        whenever(bookingParticipantRepository.save(expiredInvite.expire(clock.instant()))).thenReturn(
            expiredInvite.expire(clock.instant()),
        )

        val result =
            queryService.listBookingParticipants(
                ListBookingParticipantsQuery(
                    bookingId = 91L,
                    actor = ActorAuthContext(actorUserId = 101L),
                ),
            )

        assertEquals(2, result.items.size)
        assertEquals(BookingParticipantStatus.EXPIRED, result.items[1].status)
    }

    @Test
    fun `non participant without operator role is forbidden`() {
        val booking =
            Booking(
                id = 92L,
                occurrenceId = 502L,
                organizationId = 31L,
                leaderUserId = 101L,
                partySize = 2,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-10T00:00:00Z"),
            )

        whenever(bookingRepository.findById(92L)).thenReturn(booking)
        whenever(bookingParticipantRepository.findByBookingIdAndUserId(92L, 999L)).thenReturn(null)

        val exception =
            assertThrows<DomainException> {
                queryService.listBookingParticipants(
                    ListBookingParticipantsQuery(
                        bookingId = 92L,
                        actor = ActorAuthContext(actorUserId = 999L),
                    ),
                )
            }

        assertEquals(403, exception.status)
    }
}
