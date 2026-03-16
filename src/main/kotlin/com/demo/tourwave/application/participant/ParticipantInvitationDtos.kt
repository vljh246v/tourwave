package com.demo.tourwave.application.participant

import com.demo.tourwave.domain.participant.BookingParticipantStatus
import java.time.Instant

data class CreateParticipantInvitationCommand(
    val bookingId: Long,
    val actorUserId: Long,
    val inviteeUserId: Long,
    val idempotencyKey: String,
    val requestId: String? = null
)

data class ParticipantInvitationCreated(
    val id: Long,
    val bookingId: Long,
    val userId: Long,
    val status: BookingParticipantStatus,
    val invitedAt: Instant
)

data class CreateParticipantInvitationResult(
    val status: Int,
    val invitation: ParticipantInvitationCreated
)

enum class ParticipantInvitationResponseType {
    ACCEPT,
    DECLINE
}

data class RespondParticipantInvitationCommand(
    val bookingId: Long,
    val participantId: Long,
    val actorUserId: Long,
    val idempotencyKey: String,
    val responseType: ParticipantInvitationResponseType,
    val requestId: String? = null
)

data class ParticipantInvitationResponded(
    val id: Long,
    val bookingId: Long,
    val userId: Long,
    val status: BookingParticipantStatus,
    val respondedAt: Instant
)

data class RespondParticipantInvitationResult(
    val status: Int,
    val invitation: ParticipantInvitationResponded
)

data class RecordParticipantAttendanceCommand(
    val bookingId: Long,
    val participantId: Long,
    val actorUserId: Long,
    val attendanceStatus: com.demo.tourwave.domain.booking.AttendanceStatus,
    val idempotencyKey: String,
    val requestId: String? = null
)

data class ParticipantAttendanceRecorded(
    val id: Long,
    val bookingId: Long,
    val userId: Long,
    val attendanceStatus: com.demo.tourwave.domain.booking.AttendanceStatus
)

data class RecordParticipantAttendanceResult(
    val status: Int,
    val attendance: ParticipantAttendanceRecorded
)
