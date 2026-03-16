package com.demo.tourwave.application.participant

import com.demo.tourwave.application.common.port.ActorAuthContext
import com.demo.tourwave.domain.booking.AttendanceStatus
import com.demo.tourwave.domain.participant.BookingParticipantStatus
import java.time.Instant

data class ListBookingParticipantsQuery(
    val bookingId: Long,
    val actor: ActorAuthContext
)

data class BookingParticipantView(
    val id: Long,
    val bookingId: Long,
    val userId: Long,
    val status: BookingParticipantStatus,
    val attendanceStatus: AttendanceStatus,
    val invitedAt: Instant?,
    val respondedAt: Instant?
)

data class BookingParticipantListResult(
    val items: List<BookingParticipantView>
)

class ParticipantQueryService(
    private val participantAccessPolicy: ParticipantAccessPolicy,
    private val participantInvitationLifecycleService: ParticipantInvitationLifecycleService
) {
    fun listBookingParticipants(query: ListBookingParticipantsQuery): BookingParticipantListResult {
        participantAccessPolicy.authorizeBookingParticipants(
            bookingId = query.bookingId,
            actor = query.actor
        )

        val items = participantInvitationLifecycleService.refreshBookingParticipants(query.bookingId)
            .sortedBy { requireNotNull(it.id) }
            .map {
                BookingParticipantView(
                    id = requireNotNull(it.id),
                    bookingId = it.bookingId,
                    userId = it.userId,
                    status = it.status,
                    attendanceStatus = it.attendanceStatus,
                    invitedAt = it.invitedAt,
                    respondedAt = it.respondedAt
                )
            }

        return BookingParticipantListResult(items = items)
    }
}
