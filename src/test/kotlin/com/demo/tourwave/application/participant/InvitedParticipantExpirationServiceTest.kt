package com.demo.tourwave.application.participant

import com.demo.tourwave.adapter.out.persistence.audit.InMemoryAuditEventAdapter
import com.demo.tourwave.adapter.out.persistence.booking.InMemoryBookingRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.occurrence.InMemoryOccurrenceRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.participant.InMemoryBookingParticipantRepositoryAdapter
import com.demo.tourwave.application.common.TimeWindowPolicyService
import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.occurrence.Occurrence
import com.demo.tourwave.domain.participant.BookingParticipant
import com.demo.tourwave.domain.participant.BookingParticipantStatus
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InvitedParticipantExpirationServiceTest {
    private val bookingRepository = InMemoryBookingRepositoryAdapter()
    private val occurrenceRepository = InMemoryOccurrenceRepositoryAdapter()
    private val bookingParticipantRepository = InMemoryBookingParticipantRepositoryAdapter()
    private val auditEventAdapter = InMemoryAuditEventAdapter()
    private val clock = Clock.fixed(Instant.parse("2026-03-16T12:00:00Z"), ZoneOffset.UTC)

    private val lifecycleService =
        ParticipantInvitationLifecycleService(
            bookingRepository = bookingRepository,
            occurrenceRepository = occurrenceRepository,
            bookingParticipantRepository = bookingParticipantRepository,
            timeWindowPolicyService =
                TimeWindowPolicyService(
                    invitationWindowMinutes = 360,
                    invitationExpiryHours = 48,
                    refundFullWindowHours = 48,
                ),
            clock = clock,
        )

    private val service =
        InvitedParticipantExpirationService(
            participantInvitationLifecycleService = lifecycleService,
            auditEventPort = auditEventAdapter,
            clock = clock,
        )

    @Test
    fun `expire invitations marks pending invite as expired and appends job audit`() {
        occurrenceRepository.save(
            Occurrence(
                id = 5001L,
                organizationId = 31L,
                capacity = 10,
                startsAtUtc = Instant.parse("2026-03-20T12:00:00Z"),
            ),
        )
        val booking =
            bookingRepository.save(
                Booking(
                    occurrenceId = 5001L,
                    organizationId = 31L,
                    leaderUserId = 101L,
                    partySize = 2,
                    status = BookingStatus.CONFIRMED,
                    paymentStatus = PaymentStatus.PAID,
                    createdAt = Instant.parse("2026-03-10T00:00:00Z"),
                ),
            )
        val participant =
            bookingParticipantRepository.save(
                BookingParticipant(
                    bookingId = requireNotNull(booking.id),
                    userId = 202L,
                    status = BookingParticipantStatus.INVITED,
                    invitedAt = Instant.parse("2026-03-14T11:00:00Z"),
                    createdAt = Instant.parse("2026-03-14T11:00:00Z"),
                ),
            )

        val result = service.expireInvitations()

        val refreshed = bookingParticipantRepository.findById(requireNotNull(participant.id))!!
        assertEquals(listOf(requireNotNull(participant.id)), result.expiredParticipantIds)
        assertEquals(BookingParticipantStatus.EXPIRED, refreshed.status)
        assertTrue(auditEventAdapter.all().any { it.action == "PARTICIPANT_INVITATION_EXPIRED" && it.actorType.name == "JOB" })
    }
}
