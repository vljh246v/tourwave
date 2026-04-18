package com.demo.tourwave.adapter.out.persistence.jpa.participant

import com.demo.tourwave.domain.participant.BookingParticipantStatus
import org.springframework.data.jpa.repository.JpaRepository

interface BookingParticipantJpaRepository : JpaRepository<BookingParticipantJpaEntity, Long> {
    fun findByBookingIdOrderByCreatedAtAscIdAsc(bookingId: Long): List<BookingParticipantJpaEntity>

    fun findByBookingIdAndUserId(
        bookingId: Long,
        userId: Long,
    ): BookingParticipantJpaEntity?

    fun findByUserIdOrderByCreatedAtDescIdDesc(userId: Long): List<BookingParticipantJpaEntity>

    fun findByStatusOrderByCreatedAtAscIdAsc(status: BookingParticipantStatus): List<BookingParticipantJpaEntity>
}
