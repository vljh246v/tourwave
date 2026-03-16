package com.demo.tourwave.application.participant

import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.common.port.ActorAuthContext
import com.demo.tourwave.application.participant.port.BookingParticipantRepository
import com.demo.tourwave.domain.booking.AttendanceStatus
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.demo.tourwave.domain.occurrence.OccurrenceStatus
import com.demo.tourwave.domain.participant.BookingParticipantStatus

data class GetOccurrenceRosterQuery(
    val occurrenceId: Long,
    val actor: ActorAuthContext
)

data class OccurrenceRosterEntryView(
    val occurrenceId: Long,
    val bookingId: Long,
    val organizationId: Long,
    val bookingLeaderUserId: Long,
    val bookingStatus: BookingStatus,
    val participantId: Long,
    val participantUserId: Long,
    val participantStatus: BookingParticipantStatus,
    val attendanceStatus: AttendanceStatus
)

data class OccurrenceRosterResult(
    val occurrenceId: Long,
    val items: List<OccurrenceRosterEntryView>
)

class ParticipantRosterQueryService(
    private val bookingRepository: BookingRepository,
    private val occurrenceRepository: OccurrenceRepository,
    private val bookingParticipantRepository: BookingParticipantRepository
) {
    fun getOccurrenceRoster(query: GetOccurrenceRosterQuery): OccurrenceRosterResult {
        val occurrence = occurrenceRepository.getOrCreate(query.occurrenceId)
        authorizeOperatorScope(actor = query.actor, organizationId = occurrence.organizationId)

        val items = bookingRepository.findByOccurrenceId(query.occurrenceId)
            .sortedBy { requireNotNull(it.id) }
            .flatMap { booking ->
                bookingParticipantRepository.findByBookingId(requireNotNull(booking.id))
                    .sortedBy { requireNotNull(it.id) }
                    .map { participant ->
                        OccurrenceRosterEntryView(
                            occurrenceId = query.occurrenceId,
                            bookingId = requireNotNull(booking.id),
                            organizationId = booking.organizationId,
                            bookingLeaderUserId = booking.leaderUserId,
                            bookingStatus = booking.status,
                            participantId = requireNotNull(participant.id),
                            participantUserId = participant.userId,
                            participantStatus = participant.status,
                            attendanceStatus = participant.attendanceStatus
                        )
                    }
            }

        return OccurrenceRosterResult(
            occurrenceId = query.occurrenceId,
            items = items
        )
    }

    private fun authorizeOperatorScope(actor: ActorAuthContext, organizationId: Long) {
        val normalizedRole = actor.actorOrgRole?.uppercase()
        if (normalizedRole != "ORG_ADMIN" && normalizedRole != "ORG_OWNER") {
            throw DomainException(
                errorCode = ErrorCode.FORBIDDEN,
                status = 403,
                message = "Only org operators can access roster",
                details = mapOf("actorUserId" to actor.actorUserId)
            )
        }

        if (actor.actorOrgId != organizationId) {
            throw DomainException(
                errorCode = ErrorCode.FORBIDDEN,
                status = 403,
                message = "operator organization does not match occurrence scope",
                details = mapOf(
                    "occurrenceOrganizationId" to organizationId,
                    "actorOrganizationId" to actor.actorOrgId
                )
            )
        }
    }
}
