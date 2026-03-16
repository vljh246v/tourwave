package com.demo.tourwave.application.booking

import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.common.port.ActorAuthContext
import com.demo.tourwave.application.participant.ParticipantAccessPolicy
import com.demo.tourwave.application.participant.ParticipantInvitationLifecycleService
import com.demo.tourwave.domain.booking.AttendanceStatus
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.occurrence.OccurrenceStatus
import com.demo.tourwave.domain.participant.BookingParticipantStatus
import java.time.Instant

data class GetBookingDetailQuery(
    val bookingId: Long,
    val actor: ActorAuthContext
)

data class BookingDetailView(
    val id: Long,
    val organizationId: Long,
    val occurrenceId: Long,
    val userId: Long,
    val partySize: Int,
    val status: BookingStatus,
    val paymentStatus: PaymentStatus,
    val createdAt: Instant,
    val offerExpiresAtUtc: Instant?,
    val occurrence: BookingOccurrenceView,
    val participants: List<BookingDetailParticipantView>
)

data class BookingOccurrenceView(
    val id: Long,
    val organizationId: Long,
    val tourId: Long?,
    val instructorProfileId: Long?,
    val capacity: Int,
    val startsAtUtc: Instant?,
    val status: OccurrenceStatus
)

data class BookingDetailParticipantView(
    val id: Long,
    val bookingId: Long,
    val userId: Long,
    val status: BookingParticipantStatus,
    val attendanceStatus: AttendanceStatus,
    val invitedAt: Instant?,
    val respondedAt: Instant?
)

class BookingQueryService(
    private val bookingRepository: BookingRepository,
    private val occurrenceRepository: OccurrenceRepository,
    private val participantAccessPolicy: ParticipantAccessPolicy,
    private val participantInvitationLifecycleService: ParticipantInvitationLifecycleService
) {
    fun getBookingDetail(query: GetBookingDetailQuery): BookingDetailView {
        participantAccessPolicy.authorizeBookingParticipants(
            bookingId = query.bookingId,
            actor = query.actor
        )

        val booking = bookingRepository.findById(query.bookingId)
            ?: error("Booking access policy must guarantee booking existence")
        val occurrence = occurrenceRepository.getOrCreate(booking.occurrenceId)
        val participants = participantInvitationLifecycleService.refreshBookingParticipants(query.bookingId)
            .sortedBy { requireNotNull(it.id) }
            .map {
                BookingDetailParticipantView(
                    id = requireNotNull(it.id),
                    bookingId = it.bookingId,
                    userId = it.userId,
                    status = it.status,
                    attendanceStatus = it.attendanceStatus,
                    invitedAt = it.invitedAt,
                    respondedAt = it.respondedAt
                )
            }

        return BookingDetailView(
            id = requireNotNull(booking.id),
            organizationId = booking.organizationId,
            occurrenceId = booking.occurrenceId,
            userId = booking.leaderUserId,
            partySize = booking.partySize,
            status = booking.status,
            paymentStatus = booking.paymentStatus,
            createdAt = booking.createdAt,
            offerExpiresAtUtc = booking.offerExpiresAtUtc,
            occurrence = BookingOccurrenceView(
                id = occurrence.id,
                organizationId = occurrence.organizationId,
                tourId = occurrence.tourId,
                instructorProfileId = occurrence.instructorProfileId,
                capacity = occurrence.capacity,
                startsAtUtc = occurrence.startsAtUtc,
                status = occurrence.status
            ),
            participants = participants
        )
    }
}
