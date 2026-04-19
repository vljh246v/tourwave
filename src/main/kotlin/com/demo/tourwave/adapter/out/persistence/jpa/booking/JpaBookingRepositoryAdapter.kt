package com.demo.tourwave.adapter.out.persistence.jpa.booking

import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
@Profile("mysql", "mysql-test")
class JpaBookingRepositoryAdapter(
    private val bookingJpaRepository: BookingJpaRepository,
) : BookingRepository {
    override fun save(booking: Booking): Booking = bookingJpaRepository.save(booking.toEntity()).toDomain()

    override fun findById(bookingId: Long): Booking? = bookingJpaRepository.findById(bookingId).orElse(null)?.toDomain()

    override fun findAll(): List<Booking> = bookingJpaRepository.findAll().map { it.toDomain() }

    override fun findByLeaderUserId(userId: Long): List<Booking> =
        bookingJpaRepository.findByLeaderUserIdOrderByCreatedAtDescIdDesc(userId).map { it.toDomain() }

    override fun findByOccurrenceId(occurrenceId: Long): List<Booking> = bookingJpaRepository.findByOccurrenceId(occurrenceId).map { it.toDomain() }

    override fun findByOccurrenceAndStatuses(
        occurrenceId: Long,
        statuses: Set<BookingStatus>,
    ): List<Booking> = bookingJpaRepository.findByOccurrenceIdAndStatusIn(occurrenceId, statuses).map { it.toDomain() }

    override fun findWaitlistedByOccurrenceOrdered(occurrenceId: Long): List<Booking> =
        bookingJpaRepository.findWaitlistedOrdered(occurrenceId).map { it.toDomain() }

    override fun findExpiredOffers(now: Instant): List<Booking> = bookingJpaRepository.findExpiredOffers(now).map { it.toDomain() }

    override fun clear() {
        bookingJpaRepository.deleteAllInBatch()
    }
}

private fun Booking.toEntity(): BookingJpaEntity =
    BookingJpaEntity(
        id = id,
        occurrenceId = occurrenceId,
        organizationId = organizationId,
        leaderUserId = leaderUserId,
        partySize = partySize,
        status = status,
        paymentStatus = paymentStatus,
        offerExpiresAtUtc = offerExpiresAtUtc,
        waitlistSkipCount = waitlistSkipCount,
        lastWaitlistActionAtUtc = lastWaitlistActionAtUtc,
        createdAt = createdAt,
    )

private fun BookingJpaEntity.toDomain(): Booking =
    Booking(
        id = id,
        occurrenceId = occurrenceId,
        organizationId = organizationId,
        leaderUserId = leaderUserId,
        partySize = partySize,
        status = status,
        paymentStatus = paymentStatus,
        offerExpiresAtUtc = offerExpiresAtUtc,
        waitlistSkipCount = waitlistSkipCount,
        lastWaitlistActionAtUtc = lastWaitlistActionAtUtc,
        createdAt = createdAt,
    )
