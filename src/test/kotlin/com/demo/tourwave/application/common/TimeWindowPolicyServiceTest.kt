package com.demo.tourwave.application.common

import com.demo.tourwave.domain.occurrence.Occurrence
import com.demo.tourwave.domain.participant.BookingParticipant
import com.demo.tourwave.domain.participant.BookingParticipantStatus
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TimeWindowPolicyServiceTest {
    private val service = TimeWindowPolicyService()

    @Test
    fun `refund deadline uses local timezone across dst transition`() {
        val occurrence =
            Occurrence(
                id = 7001L,
                organizationId = 31L,
                capacity = 10,
                startsAtUtc = Instant.parse("2026-03-09T07:00:00Z"),
                timezone = "America/Los_Angeles",
            )

        val deadline = service.fullRefundDeadline(occurrence)

        assertEquals(Instant.parse("2026-03-07T07:00:00Z"), deadline)
    }

    @Test
    fun `invitation expiration uses earlier of 48 hours and start time`() {
        val occurrence =
            Occurrence(
                id = 7002L,
                organizationId = 31L,
                capacity = 10,
                startsAtUtc = Instant.parse("2026-03-17T00:00:00Z"),
                timezone = "Asia/Seoul",
            )
        val participant =
            BookingParticipant(
                bookingId = 99L,
                userId = 123L,
                status = BookingParticipantStatus.INVITED,
                invitedAt = Instant.parse("2026-03-16T10:00:00Z"),
                createdAt = Instant.parse("2026-03-16T10:00:00Z"),
            )

        assertEquals(Instant.parse("2026-03-17T00:00:00Z"), service.invitationExpiresAt(participant, occurrence))
        assertTrue(service.isInvitationExpired(participant, occurrence, Instant.parse("2026-03-17T00:00:00Z")))
        assertFalse(service.isInvitationExpired(participant, occurrence, Instant.parse("2026-03-16T23:59:59Z")))
    }
}
