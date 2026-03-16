package com.demo.tourwave.application.booking

import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.common.port.ActorAuthContext
import com.demo.tourwave.application.common.port.ActorRole
import com.demo.tourwave.application.common.port.AuditEventPort
import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.occurrence.Occurrence
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

class WaitlistOperatorServiceTest {
    private val bookingRepository = mock(BookingRepository::class.java)
    private val occurrenceRepository = mock(OccurrenceRepository::class.java)
    private val auditEventPort = mock(AuditEventPort::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-03-16T12:00:00Z"), ZoneOffset.UTC)

    private val waitlistOperatorService = WaitlistOperatorService(
        bookingRepository = bookingRepository,
        occurrenceRepository = occurrenceRepository,
        auditEventPort = auditEventPort,
        clock = clock
    )

    @Test
    fun `skip increments waitlist skip count`() {
        val booking = Booking(
            id = 91L,
            occurrenceId = 1001L,
            organizationId = 31L,
            leaderUserId = 101L,
            partySize = 2,
            status = BookingStatus.WAITLISTED,
            paymentStatus = PaymentStatus.AUTHORIZED,
            createdAt = Instant.parse("2026-03-10T00:00:00Z")
        )
        whenever(bookingRepository.findById(91L)).thenReturn(booking)
        whenever(bookingRepository.save(any())).thenAnswer { it.getArgument(0) }

        val result = waitlistOperatorService.skip(
            ManualWaitlistActionCommand(
                bookingId = 91L,
                actor = ActorAuthContext(actorUserId = 900L, roles = setOf(ActorRole.USER, ActorRole.ORG_ADMIN), actorOrgId = 31L),
                note = "잠시 뒤로"
            )
        )

        assertEquals(1, result.booking.waitlistSkipCount)
    }

    @Test
    fun `promote rejects when there is not enough capacity`() {
        val target = Booking(
            id = 92L,
            occurrenceId = 1002L,
            organizationId = 31L,
            leaderUserId = 102L,
            partySize = 3,
            status = BookingStatus.WAITLISTED,
            paymentStatus = PaymentStatus.AUTHORIZED,
            createdAt = Instant.parse("2026-03-10T00:00:00Z")
        )
        val occupied = Booking(
            id = 93L,
            occurrenceId = 1002L,
            organizationId = 31L,
            leaderUserId = 103L,
            partySize = 3,
            status = BookingStatus.CONFIRMED,
            paymentStatus = PaymentStatus.PAID,
            createdAt = Instant.parse("2026-03-10T00:01:00Z")
        )
        whenever(bookingRepository.findById(92L)).thenReturn(target)
        whenever(occurrenceRepository.getOrCreate(1002L)).thenReturn(
            Occurrence(id = 1002L, organizationId = 31L, capacity = 5)
        )
        whenever(
            bookingRepository.findByOccurrenceAndStatuses(
                occurrenceId = 1002L,
                statuses = setOf(BookingStatus.CONFIRMED, BookingStatus.OFFERED)
            )
        ).thenReturn(listOf(occupied))

        val exception = assertThrows<DomainException> {
            waitlistOperatorService.promote(
                ManualWaitlistActionCommand(
                    bookingId = 92L,
                    actor = ActorAuthContext(actorUserId = 900L, roles = setOf(ActorRole.USER, ActorRole.ORG_ADMIN), actorOrgId = 31L)
                )
            )
        }

        assertEquals(409, exception.status)
    }

    @Test
    fun `skip rejects org member role`() {
        val booking = Booking(
            id = 91L,
            occurrenceId = 1001L,
            organizationId = 31L,
            leaderUserId = 101L,
            partySize = 2,
            status = BookingStatus.WAITLISTED,
            paymentStatus = PaymentStatus.AUTHORIZED,
            createdAt = Instant.parse("2026-03-10T00:00:00Z")
        )
        whenever(bookingRepository.findById(91L)).thenReturn(booking)

        val exception = assertThrows<DomainException> {
            waitlistOperatorService.skip(
                ManualWaitlistActionCommand(
                    bookingId = 91L,
                    actor = ActorAuthContext(actorUserId = 900L, roles = setOf(ActorRole.USER, ActorRole.ORG_MEMBER), actorOrgId = 31L)
                )
            )
        }

        assertEquals(403, exception.status)
    }
}
