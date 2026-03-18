package com.demo.tourwave.adapter.out.persistence.participant

import com.demo.tourwave.application.participant.port.BookingParticipantRepository
import com.demo.tourwave.domain.participant.BookingParticipant
import com.demo.tourwave.domain.participant.BookingParticipantStatus
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Repository
@Profile("!mysql & !mysql-test")
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

    override fun findByUserId(userId: Long): List<BookingParticipant> {
        return participants.values
            .filter { it.userId == userId }
            .sortedWith(compareByDescending<BookingParticipant> { it.createdAt }.thenByDescending { it.id ?: Long.MIN_VALUE })
    }

    override fun findByStatus(status: BookingParticipantStatus): List<BookingParticipant> {
        return participants.values
            .filter { it.status == status }
            .sortedWith(compareBy<BookingParticipant> { it.createdAt }.thenBy { it.id ?: Long.MAX_VALUE })
    }

    override fun clear() {
        participants.clear()
        sequence.set(0)
    }
}
