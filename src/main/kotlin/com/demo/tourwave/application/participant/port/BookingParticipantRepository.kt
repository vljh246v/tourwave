package com.demo.tourwave.application.participant.port

import com.demo.tourwave.domain.participant.BookingParticipant
import com.demo.tourwave.domain.participant.BookingParticipantStatus

interface BookingParticipantRepository {
    fun save(participant: BookingParticipant): BookingParticipant
    fun findById(participantId: Long): BookingParticipant?
    fun findByBookingId(bookingId: Long): List<BookingParticipant>
    fun findByBookingIdAndUserId(bookingId: Long, userId: Long): BookingParticipant?
    fun findByUserId(userId: Long): List<BookingParticipant>
    fun findByStatus(status: BookingParticipantStatus): List<BookingParticipant>
    fun clear()
}
