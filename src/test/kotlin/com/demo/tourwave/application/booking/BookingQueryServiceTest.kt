package com.demo.tourwave.application.booking

import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.common.TimeWindowPolicyService
import com.demo.tourwave.application.common.port.ActorAuthContext
import com.demo.tourwave.application.participant.ParticipantAccessPolicy
import com.demo.tourwave.application.participant.ParticipantInvitationLifecycleService
import com.demo.tourwave.application.participant.port.BookingParticipantRepository
import com.demo.tourwave.domain.booking.AttendanceStatus
import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.occurrence.Occurrence
import com.demo.tourwave.domain.occurrence.OccurrenceStatus
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

class BookingQueryServiceTest {
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

    private val participantAccessPolicy =
        ParticipantAccessPolicy(
            bookingRepository = bookingRepository,
            bookingParticipantRepository = bookingParticipantRepository,
        )

    private val participantInvitationLifecycleService =
        ParticipantInvitationLifecycleService(
            bookingRepository = bookingRepository,
            occurrenceRepository = occurrenceRepository,
            bookingParticipantRepository = bookingParticipantRepository,
            timeWindowPolicyService = timeWindowPolicyService,
            clock = clock,
        )

    private val bookingQueryService =
        BookingQueryService(
            bookingRepository = bookingRepository,
            occurrenceRepository = occurrenceRepository,
            participantAccessPolicy = participantAccessPolicy,
            participantInvitationLifecycleService = participantInvitationLifecycleService,
        )

    @Test
    fun `participant can load booking detail with occurrence and participants`() {
        val booking =
            Booking(
                id = 41L,
                occurrenceId = 601L,
                organizationId = 31L,
                leaderUserId = 101L,
                partySize = 3,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-10T00:00:00Z"),
            )
        val occurrence =
            Occurrence(
                id = 601L,
                organizationId = 31L,
                tourId = 801L,
                instructorProfileId = 901L,
                capacity = 10,
                startsAtUtc = Instant.parse("2026-03-20T09:00:00Z"),
                status = OccurrenceStatus.SCHEDULED,
            )
        val leader =
            BookingParticipant.leader(
                bookingId = 41L,
                userId = 101L,
                createdAt = Instant.parse("2026-03-10T00:00:00Z"),
            ).copy(id = 1L)
        val accepted =
            BookingParticipant(
                id = 2L,
                bookingId = 41L,
                userId = 202L,
                status = BookingParticipantStatus.ACCEPTED,
                attendanceStatus = AttendanceStatus.ATTENDED,
                invitedAt = Instant.parse("2026-03-11T00:00:00Z"),
                respondedAt = Instant.parse("2026-03-11T01:00:00Z"),
                createdAt = Instant.parse("2026-03-11T00:00:00Z"),
            )

        whenever(bookingRepository.findById(41L)).thenReturn(booking)
        whenever(occurrenceRepository.getOrCreate(601L)).thenReturn(occurrence)
        whenever(bookingParticipantRepository.findByBookingIdAndUserId(41L, 101L)).thenReturn(leader)
        whenever(bookingParticipantRepository.findByBookingId(41L)).thenReturn(listOf(leader, accepted))

        val result =
            bookingQueryService.getBookingDetail(
                GetBookingDetailQuery(
                    bookingId = 41L,
                    actor = ActorAuthContext(actorUserId = 101L),
                ),
            )

        assertEquals(41L, result.id)
        assertEquals(601L, result.occurrence.id)
        assertEquals(801L, result.occurrence.tourId)
        assertEquals(901L, result.occurrence.instructorProfileId)
        assertEquals(2, result.participants.size)
        assertEquals(AttendanceStatus.ATTENDED, result.participants[1].attendanceStatus)
    }

    @Test
    fun `non participant without operator role cannot load booking detail`() {
        val booking =
            Booking(
                id = 42L,
                occurrenceId = 602L,
                organizationId = 31L,
                leaderUserId = 101L,
                partySize = 2,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-10T00:00:00Z"),
            )

        whenever(bookingRepository.findById(42L)).thenReturn(booking)
        whenever(bookingParticipantRepository.findByBookingIdAndUserId(42L, 999L)).thenReturn(null)

        val exception =
            assertThrows<DomainException> {
                bookingQueryService.getBookingDetail(
                    GetBookingDetailQuery(
                        bookingId = 42L,
                        actor = ActorAuthContext(actorUserId = 999L),
                    ),
                )
            }

        assertEquals(403, exception.status)
    }
}
