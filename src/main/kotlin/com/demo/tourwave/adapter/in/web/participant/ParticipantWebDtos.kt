package com.demo.tourwave.adapter.`in`.web.participant

import com.demo.tourwave.domain.booking.AttendanceStatus
import com.demo.tourwave.domain.participant.BookingParticipantStatus
import java.time.Instant

data class ParticipantInvitationCreateWebRequest(
    val inviteeUserId: Long,
)

data class ParticipantInvitationWebResponse(
    val id: Long,
    val bookingId: Long,
    val userId: Long,
    val status: BookingParticipantStatus,
    val invitedAt: Instant,
)

data class ParticipantInvitationResponseWebResponse(
    val id: Long,
    val bookingId: Long,
    val userId: Long,
    val status: BookingParticipantStatus,
    val respondedAt: Instant,
)

data class ParticipantAttendanceRecordWebRequest(
    val attendanceStatus: AttendanceStatus,
)

data class ParticipantAttendanceWebResponse(
    val id: Long,
    val bookingId: Long,
    val userId: Long,
    val attendanceStatus: AttendanceStatus,
)

data class ParticipantListWebResponse(
    val items: List<ParticipantDetailWebResponse>,
)

data class ParticipantDetailWebResponse(
    val id: Long,
    val bookingId: Long,
    val userId: Long,
    val status: BookingParticipantStatus,
    val attendanceStatus: AttendanceStatus,
    val invitedAt: Instant?,
    val respondedAt: Instant?,
)
