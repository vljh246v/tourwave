package com.demo.tourwave.application.participant

import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.participant.port.BookingParticipantRepository
import com.demo.tourwave.domain.participant.BookingParticipant
import com.demo.tourwave.domain.participant.BookingParticipantStatus
import java.time.Clock
import java.time.Instant

class ParticipantInvitationLifecycleService(
    private val bookingRepository: BookingRepository,
    private val occurrenceRepository: OccurrenceRepository,
    private val bookingParticipantRepository: BookingParticipantRepository,
    private val clock: Clock
) {
    fun refreshBookingParticipants(bookingId: Long): List<BookingParticipant> {
        val occurrenceStartsAtUtc = bookingRepository.findById(bookingId)
            ?.let { booking -> occurrenceRepository.getOrCreate(booking.occurrenceId).startsAtUtc }
        val now = clock.instant()

        return bookingParticipantRepository.findByBookingId(bookingId)
            .map { participant ->
                if (shouldExpire(participant, occurrenceStartsAtUtc, now)) {
                    bookingParticipantRepository.save(participant.expire(now))
                } else {
                    participant
                }
            }
    }

    fun refreshParticipant(bookingId: Long, participantId: Long): BookingParticipant? {
        return refreshBookingParticipants(bookingId).firstOrNull { it.id == participantId }
    }

    private fun shouldExpire(
        participant: BookingParticipant,
        occurrenceStartsAtUtc: Instant?,
        now: Instant
    ): Boolean {
        if (participant.status != BookingParticipantStatus.INVITED) {
            return false
        }

        val invitedAt = participant.invitedAt ?: return false
        if (!now.isBefore(invitedAt.plusSeconds(48 * 60 * 60L))) {
            return true
        }

        val sixHoursBeforeStart = occurrenceStartsAtUtc?.minusSeconds(6 * 60 * 60L)
        return sixHoursBeforeStart != null && !now.isBefore(sixHoursBeforeStart)
    }
}
