package com.demo.tourwave.application.participant

import com.demo.tourwave.application.common.port.AuditEventCommand
import com.demo.tourwave.application.common.port.AuditEventPort
import com.demo.tourwave.domain.participant.BookingParticipant
import java.time.Clock

data class InvitationExpirationJobResult(
    val expiredParticipantIds: List<Long>,
)

class InvitedParticipantExpirationService(
    private val participantInvitationLifecycleService: ParticipantInvitationLifecycleService,
    private val auditEventPort: AuditEventPort,
    private val clock: Clock,
) {
    fun expireInvitations(): InvitationExpirationJobResult {
        val expired = participantInvitationLifecycleService.expirePendingInvitations()
        expired.forEach { participant ->
            auditEventPort.append(
                AuditEventCommand(
                    actor = "JOB",
                    action = "PARTICIPANT_INVITATION_EXPIRED",
                    resourceType = "BOOKING_PARTICIPANT",
                    resourceId = requireNotNull(participant.id),
                    occurredAtUtc = clock.instant(),
                    reasonCode = "INVITATION_TIMEOUT",
                    afterJson = participantSnapshot(participant),
                ),
            )
        }
        return InvitationExpirationJobResult(expiredParticipantIds = expired.mapNotNull { it.id })
    }

    private fun participantSnapshot(participant: BookingParticipant): Map<String, Any?> {
        return mapOf(
            "bookingId" to participant.bookingId,
            "userId" to participant.userId,
            "status" to participant.status.name,
            "attendanceStatus" to participant.attendanceStatus.name,
            "invitedAt" to participant.invitedAt?.toString(),
            "respondedAt" to participant.respondedAt?.toString(),
        )
    }
}
