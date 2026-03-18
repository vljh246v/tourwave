package com.demo.tourwave.adapter.out.persistence.jpa.booking

import com.demo.tourwave.domain.booking.BookingStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface BookingJpaRepository : JpaRepository<BookingJpaEntity, Long> {
    fun findByLeaderUserIdOrderByCreatedAtDescIdDesc(leaderUserId: Long): List<BookingJpaEntity>
    fun findByOccurrenceId(occurrenceId: Long): List<BookingJpaEntity>
    fun findByOccurrenceIdAndStatusIn(occurrenceId: Long, statuses: Collection<BookingStatus>): List<BookingJpaEntity>

    @Query(
        """
        select b from BookingJpaEntity b
        where b.occurrenceId = :occurrenceId and b.status = com.demo.tourwave.domain.booking.BookingStatus.WAITLISTED
        order by b.waitlistSkipCount asc, b.lastWaitlistActionAtUtc asc nulls first, b.createdAt asc, b.id asc
        """
    )
    fun findWaitlistedOrdered(@Param("occurrenceId") occurrenceId: Long): List<BookingJpaEntity>

    @Query(
        """
        select b from BookingJpaEntity b
        where b.status = com.demo.tourwave.domain.booking.BookingStatus.OFFERED and b.offerExpiresAtUtc < :now
        order by b.offerExpiresAtUtc asc, b.id asc
        """
    )
    fun findExpiredOffers(@Param("now") now: Instant): List<BookingJpaEntity>
}
