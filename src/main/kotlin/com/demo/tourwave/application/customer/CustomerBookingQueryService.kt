package com.demo.tourwave.application.customer

import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.common.port.ActorAuthContext
import com.demo.tourwave.application.participant.ParticipantAccessPolicy
import com.demo.tourwave.application.participant.port.BookingParticipantRepository
import com.demo.tourwave.application.organization.port.OrganizationRepository
import com.demo.tourwave.application.tour.port.TourRepository
import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.demo.tourwave.domain.occurrence.Occurrence
import com.demo.tourwave.domain.occurrence.OccurrenceStatus
import com.demo.tourwave.domain.tour.TourStatus
import java.time.Instant
import java.time.format.DateTimeFormatter

data class MyBookingListItem(
    val bookingId: Long,
    val occurrenceId: Long,
    val organizationId: Long,
    val organizationName: String?,
    val tourId: Long?,
    val tourTitle: String?,
    val partySize: Int,
    val status: BookingStatus,
    val paymentStatus: PaymentStatus,
    val occurrenceStartsAtUtc: Instant?,
    val occurrenceEndsAtUtc: Instant?,
    val timezone: String?,
    val locationText: String?,
    val meetingPoint: String?,
    val createdAt: Instant
)

data class CalendarDocument(
    val fileName: String,
    val body: String
)

class CustomerBookingQueryService(
    private val bookingRepository: BookingRepository,
    private val occurrenceRepository: OccurrenceRepository,
    private val bookingParticipantRepository: BookingParticipantRepository,
    private val participantAccessPolicy: ParticipantAccessPolicy,
    private val tourRepository: TourRepository,
    private val organizationRepository: OrganizationRepository
) {
    fun listMyBookings(userId: Long): List<MyBookingListItem> {
        val leaderBookings = bookingRepository.findByLeaderUserId(userId)
        val participantBookings = bookingParticipantRepository.findByUserId(userId)
            .mapNotNull { participant -> bookingRepository.findById(participant.bookingId) }

        return (leaderBookings + participantBookings)
            .associateBy { requireNotNull(it.id) }
            .values
            .map { booking ->
                val occurrence = occurrenceRepository.findById(booking.occurrenceId) ?: occurrenceRepository.getOrCreate(booking.occurrenceId)
                val organization = organizationRepository.findById(booking.organizationId)
                val tour = occurrence.tourId?.let(tourRepository::findById)
                MyBookingListItem(
                    bookingId = requireNotNull(booking.id),
                    occurrenceId = booking.occurrenceId,
                    organizationId = booking.organizationId,
                    organizationName = organization?.name,
                    tourId = tour?.id,
                    tourTitle = tour?.title,
                    partySize = booking.partySize,
                    status = booking.status,
                    paymentStatus = booking.paymentStatus,
                    occurrenceStartsAtUtc = occurrence.startsAtUtc,
                    occurrenceEndsAtUtc = occurrence.endsAtUtc,
                    timezone = occurrence.timezone,
                    locationText = occurrence.locationText,
                    meetingPoint = occurrence.meetingPoint,
                    createdAt = booking.createdAt
                )
            }
            .sortedWith(
                compareByDescending<MyBookingListItem> { it.occurrenceStartsAtUtc ?: it.createdAt }
                    .thenByDescending { it.bookingId }
            )
    }

    fun bookingCalendar(bookingId: Long, actor: ActorAuthContext): CalendarDocument {
        participantAccessPolicy.authorizeBookingParticipants(bookingId = bookingId, actor = actor)
        val booking = bookingRepository.findById(bookingId) ?: throw notFound("booking $bookingId not found")
        val occurrence = occurrenceRepository.findById(booking.occurrenceId) ?: occurrenceRepository.getOrCreate(booking.occurrenceId)
        return buildCalendar(
            uid = "booking-$bookingId@tourwave",
            summary = buildSummary(booking, occurrence),
            description = buildBookingDescription(booking, occurrence),
            location = occurrence.locationText ?: occurrence.meetingPoint ?: "Tourwave",
            startsAtUtc = occurrence.startsAtUtc,
            endsAtUtc = occurrence.endsAtUtc,
            fileName = "booking-$bookingId.ics"
        )
    }

    fun occurrenceCalendar(occurrenceId: Long): CalendarDocument {
        val occurrence = occurrenceRepository.findById(occurrenceId) ?: throw notFound("occurrence $occurrenceId not found")
        if (occurrence.status != OccurrenceStatus.SCHEDULED) {
            throw notFound("occurrence $occurrenceId not found")
        }
        val tour = occurrence.tourId?.let(tourRepository::findById)
        if (tour == null || tour.status != TourStatus.PUBLISHED) {
            throw notFound("occurrence $occurrenceId not found")
        }
        return buildCalendar(
            uid = "occurrence-$occurrenceId@tourwave",
            summary = tour.title,
            description = tour.summary ?: "Tourwave occurrence",
            location = occurrence.locationText ?: occurrence.meetingPoint ?: "Tourwave",
            startsAtUtc = occurrence.startsAtUtc,
            endsAtUtc = occurrence.endsAtUtc,
            fileName = "occurrence-$occurrenceId.ics"
        )
    }

    private fun buildSummary(booking: Booking, occurrence: Occurrence): String {
        val tourTitle = occurrence.tourId?.let(tourRepository::findById)?.title ?: "Tour booking"
        return "$tourTitle (${booking.status.name})"
    }

    private fun buildBookingDescription(booking: Booking, occurrence: Occurrence): String {
        val tour = occurrence.tourId?.let(tourRepository::findById)
        return buildString {
            append(tour?.summary ?: "Tourwave booking")
            append("\\nParty size: ${booking.partySize}")
            append("\\nPayment: ${booking.paymentStatus.name}")
            occurrence.meetingPoint?.let { append("\\nMeeting point: $it") }
        }
    }

    private fun buildCalendar(
        uid: String,
        summary: String,
        description: String,
        location: String,
        startsAtUtc: Instant?,
        endsAtUtc: Instant?,
        fileName: String
    ): CalendarDocument {
        val start = startsAtUtc ?: throw notFound("calendar export requires a scheduled start time")
        val end = endsAtUtc ?: start.plusSeconds(7200)
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(java.time.ZoneOffset.UTC)
        val body = """
BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//Tourwave//Booking Calendar//EN
BEGIN:VEVENT
UID:$uid
DTSTAMP:${formatter.format(Instant.now())}
DTSTART:${formatter.format(start)}
DTEND:${formatter.format(end)}
SUMMARY:${escapeIcs(summary)}
DESCRIPTION:${escapeIcs(description)}
LOCATION:${escapeIcs(location)}
END:VEVENT
END:VCALENDAR
        """.trimIndent()
        return CalendarDocument(fileName = fileName, body = body)
    }

    private fun escapeIcs(value: String): String =
        value.replace("\\", "\\\\").replace(",", "\\,").replace(";", "\\;").replace("\n", "\\n")

    private fun notFound(message: String) = DomainException(
        errorCode = ErrorCode.VALIDATION_ERROR,
        status = 404,
        message = message
    )
}
