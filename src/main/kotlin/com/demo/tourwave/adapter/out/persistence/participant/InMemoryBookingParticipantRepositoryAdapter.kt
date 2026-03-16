package com.demo.tourwave.adapter.out.persistence.participant

import com.demo.tourwave.application.participant.port.BookingParticipantRepository
import com.demo.tourwave.domain.participant.BookingParticipant
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Repository
class InMemoryBookingParticipantRepositoryAdapter : BookingParticipantRepository {
    private val sequence = AtomicLong(0)
    private val participants = ConcurrentHashMap<Long, BookingParticipant>()

    override fun save(participant: BookingParticipant): BookingParticipant {
        val participantId = participant.id ?: sequence.incrementAndGet()
        val saved = participant.copy(id = participantId)
        participants[participantId] = saved
        return saved
    }

    override fun findById(participantId: Long): BookingParticipant? {
        return participants[participantId]
    }

    override fun findByBookingId(bookingId: Long): List<BookingParticipant> {
        return participants.values
            .filter { it.bookingId == bookingId }
            .sortedWith(compareBy<BookingParticipant> { it.createdAt }.thenBy { it.id ?: Long.MAX_VALUE })
    }

    override fun findByBookingIdAndUserId(bookingId: Long, userId: Long): BookingParticipant? {
        return participants.values.firstOrNull { it.bookingId == bookingId && it.userId == userId }
    }

    override fun clear() {
        participants.clear()
        sequence.set(0)
    }
}
