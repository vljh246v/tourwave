package com.demo.tourwave.application.booking

import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.common.port.ActorAuthContext
import com.demo.tourwave.application.common.port.AuditEventCommand
import com.demo.tourwave.application.common.port.AuditEventPort
import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.demo.tourwave.domain.occurrence.OccurrenceStatus
import java.time.Clock

data class ManualWaitlistActionCommand(
    val bookingId: Long,
    val actor: ActorAuthContext,
    val note: String? = null,
    val requestId: String? = null
)

data class ManualWaitlistActionResult(
    val status: Int,
    val booking: Booking
)

class WaitlistOperatorService(
    private val bookingRepository: BookingRepository,
    private val occurrenceRepository: OccurrenceRepository,
    private val auditEventPort: AuditEventPort,
    private val clock: Clock
) {
    fun promote(command: ManualWaitlistActionCommand): ManualWaitlistActionResult {
        val booking = requireOperatorScopedWaitlistedBooking(command)
        val occurrence = occurrenceRepository.getOrCreate(booking.occurrenceId)
        if (occurrence.status == OccurrenceStatus.CANCELED) {
            throw DomainException(
                errorCode = ErrorCode.OCCURRENCE_ALREADY_CANCELED,
                status = 409,
                message = "Occurrence is already canceled",
                details = mapOf("occurrenceId" to occurrence.id)
            )
        }

        val availableSeats = availableSeatsForOccurrence(
            occurrenceId = booking.occurrenceId,
            excludeBookingId = requireNotNull(booking.id)
        )

        if (booking.partySize > availableSeats) {
            throw DomainException(
                errorCode = ErrorCode.CAPACITY_EXCEEDED,
                status = 409,
                message = "Not enough seats to promote waitlist booking",
                details = mapOf(
                    "bookingId" to booking.id,
                    "partySize" to booking.partySize,
                    "availableSeats" to availableSeats
                )
            )
        }

        val promoted = bookingRepository.save(
            booking.offer(clock.instant().plusSeconds(24 * 60 * 60L))
        )
        appendAudit(
            actor = command.actor,
            booking = promoted,
            action = "WAITLIST_PROMOTED_MANUALLY",
            note = command.note,
            requestId = command.requestId
        )
        return ManualWaitlistActionResult(status = 200, booking = promoted)
    }

    fun skip(command: ManualWaitlistActionCommand): ManualWaitlistActionResult {
        val booking = requireOperatorScopedWaitlistedBooking(command)
        val skipped = bookingRepository.save(
            booking.skipWaitlist(clock.instant())
        )
        appendAudit(
            actor = command.actor,
            booking = skipped,
            action = "WAITLIST_SKIPPED_MANUALLY",
            note = command.note,
            requestId = command.requestId
        )
        return ManualWaitlistActionResult(status = 200, booking = skipped)
    }

    private fun requireOperatorScopedWaitlistedBooking(command: ManualWaitlistActionCommand): Booking {
        val booking = bookingRepository.findById(command.bookingId)
            ?: throw DomainException(
                errorCode = ErrorCode.VALIDATION_ERROR,
                status = 404,
                message = "Booking not found",
                details = mapOf("bookingId" to command.bookingId)
            )

        if (!command.actor.isOrgOperator()) {
            throw DomainException(
                errorCode = ErrorCode.FORBIDDEN,
                status = 403,
                message = "Only org operators can control waitlist",
                details = mapOf("bookingId" to command.bookingId, "actorUserId" to command.actor.actorUserId)
            )
        }

        if (command.actor.actorOrgId != booking.organizationId) {
            throw DomainException(
                errorCode = ErrorCode.FORBIDDEN,
                status = 403,
                message = "operator organization does not match booking scope",
                details = mapOf(
                    "bookingId" to command.bookingId,
                    "bookingOrganizationId" to booking.organizationId,
                    "actorOrganizationId" to command.actor.actorOrgId
                )
            )
        }

        if (booking.status != BookingStatus.WAITLISTED) {
            throw DomainException(
                errorCode = ErrorCode.INVALID_STATE_TRANSITION,
                status = 409,
                message = "Only WAITLISTED booking can be manually controlled",
                details = mapOf("bookingId" to command.bookingId, "status" to booking.status)
            )
        }

        return booking
    }

    private fun availableSeatsForOccurrence(occurrenceId: Long, excludeBookingId: Long): Int {
        val now = clock.instant()
        val occurrence = occurrenceRepository.getOrCreate(occurrenceId)
        val occupiedSeats = bookingRepository.findByOccurrenceAndStatuses(
            occurrenceId = occurrenceId,
            statuses = setOf(BookingStatus.CONFIRMED, BookingStatus.OFFERED)
        )
            .filterNot { it.id == excludeBookingId }
            .filter {
                when (it.status) {
                    BookingStatus.CONFIRMED -> true
                    BookingStatus.OFFERED -> {
                        val expiresAt = it.offerExpiresAtUtc ?: return@filter true
                        !now.isAfter(expiresAt)
                    }

                    else -> false
                }
            }
            .sumOf { it.partySize }
        return (occurrence.capacity - occupiedSeats).coerceAtLeast(0)
    }

    private fun appendAudit(
        actor: ActorAuthContext,
        booking: Booking,
        action: String,
        note: String?,
        requestId: String?
    ) {
        auditEventPort.append(
            AuditEventCommand(
                actor = "OPERATOR:${actor.actorUserId}",
                action = action,
                resourceType = "BOOKING",
                resourceId = requireNotNull(booking.id),
                occurredAtUtc = clock.instant(),
                requestId = requestId,
                details = mapOf(
                    "bookingStatus" to booking.status.name,
                    "waitlistSkipCount" to booking.waitlistSkipCount,
                    "note" to note
                )
            )
        )
    }
}
