package com.demo.tourwave.application.participant

import com.demo.tourwave.adapter.out.persistence.audit.InMemoryAuditEventAdapter
import com.demo.tourwave.adapter.out.persistence.booking.InMemoryBookingRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.idempotency.InMemoryIdempotencyStoreAdapter
import com.demo.tourwave.adapter.out.persistence.occurrence.InMemoryOccurrenceRepositoryAdapter
import com.demo.tourwave.adapter.out.persistence.participant.InMemoryBookingParticipantRepositoryAdapter
import com.demo.tourwave.application.common.TimeWindowPolicyService
import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.occurrence.Occurrence
import com.demo.tourwave.domain.participant.BookingParticipant
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ParticipantCommandServiceTest {
    private val bookingRepository = InMemoryBookingRepositoryAdapter()
    private val occurrenceRepository = InMemoryOccurrenceRepositoryAdapter()
    private val bookingParticipantRepository = InMemoryBookingParticipantRepositoryAdapter()
    private val auditEventAdapter = InMemoryAuditEventAdapter()
    private val idempotencyStore = InMemoryIdempotencyStoreAdapter()
    private val clock = Clock.fixed(Instant.parse("2026-03-16T12:00:00Z"), ZoneOffset.UTC)
    private val timeWindowPolicyService = TimeWindowPolicyService()
    private val lifecycleService = ParticipantInvitationLifecycleService(
        bookingRepository = bookingRepository,
        occurrenceRepository = occurrenceRepository,
        bookingParticipantRepository = bookingParticipantRepository,
        timeWindowPolicyService = timeWindowPolicyService,
        clock = clock
    )
    private val service = ParticipantCommandService(
        bookingRepository = bookingRepository,
        occurrenceRepository = occurrenceRepository,
        bookingParticipantRepository = bookingParticipantRepository,
        participantInvitationLifecycleService = lifecycleService,
        timeWindowPolicyService = timeWindowPolicyService,
        idempotencyStore = idempotencyStore,
        auditEventPort = auditEventAdapter,
        clock = clock
    )

    @Test
    fun `create invitation is blocked within six hours of occurrence start`() {
        occurrenceRepository.save(
            Occurrence(
                id = 6001L,
                organizationId = 31L,
                capacity = 10,
                startsAtUtc = Instant.parse("2026-03-16T17:00:00Z"),
                timezone = "Asia/Seoul"
            )
        )
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 6001L,
                organizationId = 31L,
                leaderUserId = 101L,
                partySize = 2,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-10T00:00:00Z")
            )
        )
        bookingParticipantRepository.save(
            BookingParticipant.leader(
                bookingId = requireNotNull(booking.id),
                userId = 101L,
                createdAt = Instant.parse("2026-03-10T00:00:00Z")
            )
        )

        val exception = assertThrows<DomainException> {
            service.createInvitation(
                CreateParticipantInvitationCommand(
                    bookingId = requireNotNull(booking.id),
                    actorUserId = 101L,
                    inviteeUserId = 202L,
                    idempotencyKey = "invite-window-1"
                )
            )
        }

        assertEquals(409, exception.status)
    }

    @Test
    fun `create invitation appends structured participant audit`() {
        occurrenceRepository.save(
            Occurrence(
                id = 6002L,
                organizationId = 31L,
                capacity = 10,
                startsAtUtc = Instant.parse("2026-03-18T12:00:00Z"),
                timezone = "Asia/Seoul"
            )
        )
        val booking = bookingRepository.save(
            Booking(
                occurrenceId = 6002L,
                organizationId = 31L,
                leaderUserId = 101L,
                partySize = 2,
                status = BookingStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PAID,
                createdAt = Instant.parse("2026-03-10T00:00:00Z")
            )
        )
        bookingParticipantRepository.save(
            BookingParticipant.leader(
                bookingId = requireNotNull(booking.id),
                userId = 101L,
                createdAt = Instant.parse("2026-03-10T00:00:00Z")
            )
        )

        service.createInvitation(
            CreateParticipantInvitationCommand(
                bookingId = requireNotNull(booking.id),
                actorUserId = 101L,
                inviteeUserId = 202L,
                idempotencyKey = "invite-success-1"
            )
        )

        val audit = auditEventAdapter.all().last()
        assertEquals("PARTICIPANT_INVITATION_CREATED", audit.reasonCode)
        assertNotNull(audit.afterJson)
        assertEquals("INVITED", audit.afterJson?.get("status"))
    }
}
