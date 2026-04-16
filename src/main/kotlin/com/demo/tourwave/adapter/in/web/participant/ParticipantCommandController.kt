package com.demo.tourwave.adapter.`in`.web.participant

import com.demo.tourwave.application.common.port.AuthzGuardPort
import com.demo.tourwave.application.participant.CreateParticipantInvitationCommand
import com.demo.tourwave.application.participant.ParticipantAttendanceRecorded
import com.demo.tourwave.application.participant.ParticipantCommandService
import com.demo.tourwave.application.participant.ParticipantInvitationCreated
import com.demo.tourwave.application.participant.ParticipantInvitationResponded
import com.demo.tourwave.application.participant.ParticipantInvitationResponseType
import com.demo.tourwave.application.participant.RecordParticipantAttendanceCommand
import com.demo.tourwave.application.participant.RespondParticipantInvitationCommand
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class ParticipantCommandController(
    private val participantCommandService: ParticipantCommandService,
    private val authzGuardPort: AuthzGuardPort,
) {
    @PostMapping("/bookings/{bookingId}/participants/invitations")
    fun createInvitation(
        @PathVariable bookingId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("X-Request-Id", required = false) requestId: String?,
        @RequestBody request: ParticipantInvitationCreateWebRequest,
    ): ResponseEntity<ParticipantInvitationWebResponse> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        val result =
            participantCommandService.createInvitation(
                CreateParticipantInvitationCommand(
                    bookingId = bookingId,
                    actorUserId = requiredActorUserId,
                    inviteeUserId = request.inviteeUserId,
                    idempotencyKey = idempotencyKey,
                    requestId = requestId,
                ),
            )
        return ResponseEntity.status(result.status).body(result.invitation.toWebResponse())
    }

    @PostMapping("/bookings/{bookingId}/participants/invitations/{participantId}/accept")
    fun acceptInvitation(
        @PathVariable bookingId: Long,
        @PathVariable participantId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("X-Request-Id", required = false) requestId: String?,
    ): ResponseEntity<ParticipantInvitationResponseWebResponse> {
        return respondInvitation(
            bookingId = bookingId,
            participantId = participantId,
            idempotencyKey = idempotencyKey,
            actorUserId = actorUserId,
            requestId = requestId,
            responseType = ParticipantInvitationResponseType.ACCEPT,
        )
    }

    @PostMapping("/bookings/{bookingId}/participants/invitations/{participantId}/decline")
    fun declineInvitation(
        @PathVariable bookingId: Long,
        @PathVariable participantId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("X-Request-Id", required = false) requestId: String?,
    ): ResponseEntity<ParticipantInvitationResponseWebResponse> {
        return respondInvitation(
            bookingId = bookingId,
            participantId = participantId,
            idempotencyKey = idempotencyKey,
            actorUserId = actorUserId,
            requestId = requestId,
            responseType = ParticipantInvitationResponseType.DECLINE,
        )
    }

    @PostMapping("/bookings/{bookingId}/participants/{participantId}/attendance")
    fun recordAttendance(
        @PathVariable bookingId: Long,
        @PathVariable participantId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("X-Request-Id", required = false) requestId: String?,
        @RequestBody request: ParticipantAttendanceRecordWebRequest,
    ): ResponseEntity<ParticipantAttendanceWebResponse> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        val result =
            participantCommandService.recordAttendance(
                RecordParticipantAttendanceCommand(
                    bookingId = bookingId,
                    participantId = participantId,
                    actorUserId = requiredActorUserId,
                    attendanceStatus = request.attendanceStatus,
                    idempotencyKey = idempotencyKey,
                    requestId = requestId,
                ),
            )
        return ResponseEntity.status(result.status).body(result.attendance.toAttendanceWebResponse())
    }

    private fun respondInvitation(
        bookingId: Long,
        participantId: Long,
        idempotencyKey: String,
        actorUserId: Long?,
        requestId: String?,
        responseType: ParticipantInvitationResponseType,
    ): ResponseEntity<ParticipantInvitationResponseWebResponse> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        val result =
            participantCommandService.respondInvitation(
                RespondParticipantInvitationCommand(
                    bookingId = bookingId,
                    participantId = participantId,
                    actorUserId = requiredActorUserId,
                    idempotencyKey = idempotencyKey,
                    responseType = responseType,
                    requestId = requestId,
                ),
            )
        return ResponseEntity.status(result.status).body(result.invitation.toResponseWebResponse())
    }

    private fun ParticipantInvitationCreated.toWebResponse(): ParticipantInvitationWebResponse {
        return ParticipantInvitationWebResponse(
            id = id,
            bookingId = bookingId,
            userId = userId,
            status = status,
            invitedAt = invitedAt,
        )
    }

    private fun ParticipantInvitationResponded.toResponseWebResponse(): ParticipantInvitationResponseWebResponse {
        return ParticipantInvitationResponseWebResponse(
            id = id,
            bookingId = bookingId,
            userId = userId,
            status = status,
            respondedAt = respondedAt,
        )
    }

    private fun ParticipantAttendanceRecorded.toAttendanceWebResponse(): ParticipantAttendanceWebResponse {
        return ParticipantAttendanceWebResponse(
            id = id,
            bookingId = bookingId,
            userId = userId,
            attendanceStatus = attendanceStatus,
        )
    }
}
