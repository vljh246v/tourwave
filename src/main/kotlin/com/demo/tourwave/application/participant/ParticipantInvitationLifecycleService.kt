package com.demo.tourwave.application.participant

import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.common.TimeWindowPolicyService
import com.demo.tourwave.application.participant.port.BookingParticipantRepository
import com.demo.tourwave.domain.participant.BookingParticipant
import java.time.Clock

class ParticipantInvitationLifecycleService(
    private val bookingRepository: BookingRepository,
    private val occurrenceRepository: OccurrenceRepository,
    private val bookingParticipantRepository: BookingParticipantRepository,
    private val timeWindowPolicyService: TimeWindowPolicyService,
    private val clock: Clock,
) {
    fun refreshBookingParticipants(bookingId: Long): List<BookingParticipant> {
        val occurrence =
            bookingRepository.findById(bookingId)
                ?.let { booking -> occurrenceRepository.getOrCreate(booking.occurrenceId) }
        val now = clock.instant()

        return bookingParticipantRepository.findByBookingId(bookingId)
            .map { participant ->
                if (timeWindowPolicyService.isInvitationExpired(participant, occurrence, now)) {
                    bookingParticipantRepository.save(participant.expire(now))
                } else {
                    participant
                }
            }
    }

    fun refreshParticipant(
        bookingId: Long,
        participantId: Long,
    ): BookingParticipant? {
        return refreshBookingParticipants(bookingId).firstOrNull { it.id == participantId }
    }

    fun expirePendingInvitations(): List<BookingParticipant> {
        val now = clock.instant()
        return bookingParticipantRepository.findByStatus(com.demo.tourwave.domain.participant.BookingParticipantStatus.INVITED)
            .mapNotNull { participant ->
                val occurrence =
                    bookingRepository.findById(participant.bookingId)
                        ?.let { booking -> occurrenceRepository.getOrCreate(booking.occurrenceId) }
                if (!timeWindowPolicyService.isInvitationExpired(participant, occurrence, now)) {
                    return@mapNotNull null
                }
                bookingParticipantRepository.save(participant.expire(now))
            }
    }
}
