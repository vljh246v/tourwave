package com.demo.tourwave.application.participant

import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.common.port.ActorAuthContext
import com.demo.tourwave.application.common.port.ActorRole
import com.demo.tourwave.application.participant.port.BookingParticipantRepository
import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.occurrence.Occurrence
import com.demo.tourwave.domain.participant.BookingParticipant
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import java.time.Instant
import kotlin.test.assertEquals

class ParticipantRosterQueryServiceTest {
    private val bookingRepository = mock(BookingRepository::class.java)
    private val occurrenceRepository = mock(OccurrenceRepository::class.java)
    private val participantRepository = mock(BookingParticipantRepository::class.java)

    private val service =
        ParticipantRosterQueryService(
            bookingRepository = bookingRepository,
            occurrenceRepository = occurrenceRepository,
            bookingParticipantRepository = participantRepository,
        )

    @Test
    fun `operator can fetch occurrence roster`() {
        val booking =
            Booking(
                id = 201L,
                occurrenceId = 3001L,
                organizationId = 31L,
                leaderUserId = 101L,
                partySize = 2,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-10T00:00:00Z"),
            )
        val leader =
            BookingParticipant.leader(
                bookingId = 201L,
                userId = 101L,
                createdAt = Instant.parse("2026-03-10T00:00:00Z"),
            ).copy(id = 1L)

        whenever(occurrenceRepository.getOrCreate(3001L)).thenReturn(
            Occurrence(id = 3001L, organizationId = 31L, tourId = 801L, instructorProfileId = 901L, capacity = 10),
        )
        whenever(bookingRepository.findByOccurrenceId(3001L)).thenReturn(listOf(booking))
        whenever(participantRepository.findByBookingId(201L)).thenReturn(listOf(leader))

        val result =
            service.getOccurrenceRoster(
                GetOccurrenceRosterQuery(
                    occurrenceId = 3001L,
                    actor = ActorAuthContext(actorUserId = 900L, roles = setOf(ActorRole.USER, ActorRole.ORG_ADMIN), actorOrgId = 31L),
                ),
            )

        assertEquals(1, result.items.size)
        assertEquals(801L, result.tourId)
        assertEquals(901L, result.instructorProfileId)
        assertEquals(101L, result.items.single().participantUserId)
    }

    @Test
    fun `roster rejects mismatched org operator`() {
        whenever(occurrenceRepository.getOrCreate(3001L)).thenReturn(
            Occurrence(id = 3001L, organizationId = 31L, capacity = 10),
        )

        val exception =
            assertThrows<DomainException> {
                service.getOccurrenceRoster(
                    GetOccurrenceRosterQuery(
                        occurrenceId = 3001L,
                        actor = ActorAuthContext(actorUserId = 900L, roles = setOf(ActorRole.USER, ActorRole.ORG_ADMIN), actorOrgId = 99L),
                    ),
                )
            }

        assertEquals(403, exception.status)
    }

    @Test
    fun `roster rejects org member role`() {
        whenever(occurrenceRepository.getOrCreate(3001L)).thenReturn(
            Occurrence(id = 3001L, organizationId = 31L, capacity = 10),
        )

        val exception =
            assertThrows<DomainException> {
                service.getOccurrenceRoster(
                    GetOccurrenceRosterQuery(
                        occurrenceId = 3001L,
                        actor = ActorAuthContext(actorUserId = 900L, roles = setOf(ActorRole.USER, ActorRole.ORG_MEMBER), actorOrgId = 31L),
                    ),
                )
            }

        assertEquals(403, exception.status)
    }
}
