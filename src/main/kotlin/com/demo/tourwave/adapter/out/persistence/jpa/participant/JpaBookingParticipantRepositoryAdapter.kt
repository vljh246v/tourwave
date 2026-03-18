package com.demo.tourwave.adapter.out.persistence.jpa.participant

import com.demo.tourwave.application.participant.port.BookingParticipantRepository
import com.demo.tourwave.domain.participant.BookingParticipant
import com.demo.tourwave.domain.participant.BookingParticipantStatus
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository

@Repository
@Profile("mysql", "mysql-test")
class JpaBookingParticipantRepositoryAdapter(
    private val bookingParticipantJpaRepository: BookingParticipantJpaRepository
) : BookingParticipantRepository {
    override fun save(participant: BookingParticipant): BookingParticipant =
        bookingParticipantJpaRepository.save(participant.toEntity()).toDomain()

    override fun findById(participantId: Long): BookingParticipant? =
        bookingParticipantJpaRepository.findById(participantId).orElse(null)?.toDomain()

    override fun findByBookingId(bookingId: Long): List<BookingParticipant> =
        bookingParticipantJpaRepository.findByBookingIdOrderByCreatedAtAscIdAsc(bookingId).map { it.toDomain() }

    override fun findByBookingIdAndUserId(bookingId: Long, userId: Long): BookingParticipant? =
        bookingParticipantJpaRepository.findByBookingIdAndUserId(bookingId, userId)?.toDomain()

    override fun findByUserId(userId: Long): List<BookingParticipant> =
        bookingParticipantJpaRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId).map { it.toDomain() }

    override fun findByStatus(status: BookingParticipantStatus): List<BookingParticipant> =
        bookingParticipantJpaRepository.findByStatusOrderByCreatedAtAscIdAsc(status).map { it.toDomain() }

    override fun clear() {
        bookingParticipantJpaRepository.deleteAllInBatch()
    }
}

private fun BookingParticipant.toEntity(): BookingParticipantJpaEntity =
    BookingParticipantJpaEntity(
        id = id,
        bookingId = bookingId,
        userId = userId,
        status = status,
        attendanceStatus = attendanceStatus,
        invitedAt = invitedAt,
        respondedAt = respondedAt,
        createdAt = createdAt
    )

private fun BookingParticipantJpaEntity.toDomain(): BookingParticipant =
    BookingParticipant(
        id = id,
        bookingId = bookingId,
        userId = userId,
        status = status,
        attendanceStatus = attendanceStatus,
        invitedAt = invitedAt,
        respondedAt = respondedAt,
        createdAt = createdAt
    )
