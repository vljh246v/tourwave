package com.demo.tourwave.domain.participant

import com.demo.tourwave.domain.booking.AttendanceStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class BookingParticipantTest {

    @Test
    fun `leader factory should create leader participant with unknown attendance`() {
        val createdAt = Instant.parse("2026-03-16T00:00:00Z")

        val participant = BookingParticipant.leader(
            bookingId = 10L,
            userId = 101L,
            createdAt = createdAt
        )

        assertEquals(10L, participant.bookingId)
        assertEquals(101L, participant.userId)
        assertEquals(BookingParticipantStatus.LEADER, participant.status)
        assertEquals(AttendanceStatus.UNKNOWN, participant.attendanceStatus)
        assertNull(participant.invitedAt)
        assertNull(participant.respondedAt)
        assertEquals(createdAt, participant.createdAt)
    }

    @Test
    fun `cancel should mark active participant as canceled`() {
        val createdAt = Instant.parse("2026-03-16T00:00:00Z")
        val canceledAt = Instant.parse("2026-03-16T02:00:00Z")
        val participant = BookingParticipant.leader(
            bookingId = 10L,
            userId = 101L,
            createdAt = createdAt
        )

        val canceled = participant.cancel(canceledAt)

        assertEquals(BookingParticipantStatus.CANCELED, canceled.status)
        assertEquals(canceledAt, canceled.respondedAt)
    }

    @Test
    fun `isActive should return false for terminal participant status`() {
        val participant = BookingParticipant(
            bookingId = 10L,
            userId = 101L,
            status = BookingParticipantStatus.DECLINED
        )

        assertTrue(!participant.isActive())
    }

    @Test
    fun `accept should move invitation to accepted`() {
        val respondedAt = Instant.parse("2026-03-16T03:00:00Z")
        val participant = BookingParticipant(
            bookingId = 10L,
            userId = 101L,
            status = BookingParticipantStatus.INVITED,
            invitedAt = Instant.parse("2026-03-16T01:00:00Z")
        )

        val accepted = participant.accept(respondedAt)

        assertEquals(BookingParticipantStatus.ACCEPTED, accepted.status)
        assertEquals(respondedAt, accepted.respondedAt)
    }

    @Test
    fun `decline should move invitation to declined`() {
        val respondedAt = Instant.parse("2026-03-16T03:00:00Z")
        val participant = BookingParticipant(
            bookingId = 10L,
            userId = 101L,
            status = BookingParticipantStatus.INVITED,
            invitedAt = Instant.parse("2026-03-16T01:00:00Z")
        )

        val declined = participant.decline(respondedAt)

        assertEquals(BookingParticipantStatus.DECLINED, declined.status)
        assertEquals(respondedAt, declined.respondedAt)
    }

    @Test
    fun `expire should move invited participant to expired`() {
        val expiredAt = Instant.parse("2026-03-16T04:00:00Z")
        val participant = BookingParticipant(
            bookingId = 10L,
            userId = 101L,
            status = BookingParticipantStatus.INVITED,
            invitedAt = Instant.parse("2026-03-16T01:00:00Z")
        )

        val expired = participant.expire(expiredAt)

        assertEquals(BookingParticipantStatus.EXPIRED, expired.status)
        assertEquals(expiredAt, expired.respondedAt)
    }

    @Test
    fun `recordAttendance should update accepted participant`() {
        val participant = BookingParticipant(
            bookingId = 10L,
            userId = 101L,
            status = BookingParticipantStatus.ACCEPTED
        )

        val recorded = participant.recordAttendance(AttendanceStatus.ATTENDED)

        assertEquals(AttendanceStatus.ATTENDED, recorded.attendanceStatus)
    }
}
