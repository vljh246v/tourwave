package com.demo.tourwave.application.booking

import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.common.TimeWindowPolicyService
import com.demo.tourwave.application.common.port.AuditEventCommand
import com.demo.tourwave.application.common.port.AuditEventPort
import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.RefundPolicyAction
import com.demo.tourwave.domain.occurrence.OccurrenceStatus
import java.time.Clock

data class OfferExpirationJobResult(
    val expiredBookingIds: List<Long>,
)

class OfferExpirationService(
    private val bookingRepository: BookingRepository,
    private val occurrenceRepository: OccurrenceRepository,
    private val auditEventPort: AuditEventPort,
    private val paymentLedgerService: PaymentLedgerService,
    private val timeWindowPolicyService: TimeWindowPolicyService,
    private val clock: Clock,
) {
    companion object {
        private const val OFFER_WINDOW_SECONDS = 24 * 60 * 60L
    }

    fun expireOffers(): OfferExpirationJobResult {
        val now = clock.instant()
        val expiredBookingIds =
            bookingRepository.findExpiredOffers(now)
                .mapNotNull { booking ->
                    if (!timeWindowPolicyService.isOfferExpired(now, booking.offerExpiresAtUtc)) {
                        return@mapNotNull null
                    }

                    val expired = bookingRepository.save(booking.expireOffer())
                    val occurrence = occurrenceRepository.getOrCreate(expired.occurrenceId)
                    val settled =
                        bookingRepository.save(
                            paymentLedgerService.applyRefundPolicy(
                                booking = expired,
                                occurrence = occurrence,
                                action = RefundPolicyAction.OFFER_EXPIRED,
                                actorUserId = 0L,
                            ),
                        )
                    appendAudit(before = booking, after = settled)
                    promoteWaitlist(occurrenceId = settled.occurrenceId, now = now)
                    settled.id
                }
        return OfferExpirationJobResult(expiredBookingIds = expiredBookingIds)
    }

    private fun promoteWaitlist(
        occurrenceId: Long,
        now: java.time.Instant,
    ) {
        val occurrence = occurrenceRepository.getOrCreate(occurrenceId)
        if (occurrence.status == OccurrenceStatus.CANCELED) {
            return
        }

        var availableSeats =
            availableSeatsForOccurrence(
                occurrenceId = occurrenceId,
                excludeBookingId = null,
                now = now,
            )
        if (availableSeats <= 0) {
            return
        }

        bookingRepository.findWaitlistedByOccurrenceOrdered(occurrenceId).forEach { waitlisted ->
            if (waitlisted.partySize > availableSeats) {
                return@forEach
            }

            val promoted = bookingRepository.save(waitlisted.offer(now.plusSeconds(OFFER_WINDOW_SECONDS)))
            availableSeats -= promoted.partySize
            auditEventPort.append(
                AuditEventCommand(
                    actor = "JOB",
                    action = "WAITLIST_PROMOTED_TO_OFFER",
                    resourceType = "BOOKING",
                    resourceId = requireNotNull(promoted.id),
                    occurredAtUtc = now,
                    reasonCode = "OFFER_TIMEOUT_SEAT_RELEASE",
                    beforeJson = bookingSnapshot(waitlisted),
                    afterJson = bookingSnapshot(promoted),
                ),
            )
        }
    }

    private fun availableSeatsForOccurrence(
        occurrenceId: Long,
        excludeBookingId: Long?,
        now: java.time.Instant,
    ): Int {
        val occurrence = occurrenceRepository.getOrCreate(occurrenceId)
        val occupiedSeats =
            bookingRepository.findByOccurrenceAndStatuses(
                occurrenceId = occurrenceId,
                statuses = setOf(BookingStatus.CONFIRMED, BookingStatus.OFFERED),
            )
                .filterNot { excludeBookingId != null && it.id == excludeBookingId }
                .filter { booking -> isSeatHolding(booking, now) }
                .sumOf { it.partySize }
        return (occurrence.capacity - occupiedSeats).coerceAtLeast(0)
    }

    private fun isSeatHolding(
        booking: Booking,
        now: java.time.Instant,
    ): Boolean {
        return when (booking.status) {
            BookingStatus.CONFIRMED -> true
            BookingStatus.OFFERED -> !timeWindowPolicyService.isOfferExpired(now, booking.offerExpiresAtUtc)
            else -> false
        }
    }

    private fun appendAudit(
        before: Booking,
        after: Booking,
    ) {
        auditEventPort.append(
            AuditEventCommand(
                actor = "JOB",
                action = "OFFER_EXPIRED",
                resourceType = "BOOKING",
                resourceId = requireNotNull(after.id),
                occurredAtUtc = clock.instant(),
                reasonCode = "OFFER_TIMEOUT",
                beforeJson = bookingSnapshot(before),
                afterJson = bookingSnapshot(after),
            ),
        )
    }

    private fun bookingSnapshot(booking: Booking): Map<String, Any?> {
        return mapOf(
            "status" to booking.status.name,
            "paymentStatus" to booking.paymentStatus.name,
            "offerExpiresAtUtc" to booking.offerExpiresAtUtc?.toString(),
            "partySize" to booking.partySize,
            "waitlistSkipCount" to booking.waitlistSkipCount,
        )
    }
}
