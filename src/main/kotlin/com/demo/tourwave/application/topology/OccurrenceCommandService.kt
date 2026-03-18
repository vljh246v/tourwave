package com.demo.tourwave.application.topology

import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.topology.port.InstructorProfileRepository
import com.demo.tourwave.application.topology.port.TourRepository
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.demo.tourwave.domain.instructor.InstructorProfileStatus
import com.demo.tourwave.domain.occurrence.Occurrence
import com.demo.tourwave.domain.occurrence.OccurrenceStatus
import com.demo.tourwave.domain.tour.Tour
import java.time.Clock

class OccurrenceCommandService(
    private val occurrenceRepository: OccurrenceRepository,
    private val bookingRepository: BookingRepository,
    private val tourRepository: TourRepository,
    private val instructorProfileRepository: InstructorProfileRepository,
    private val organizationAccessGuard: OrganizationAccessGuard,
    private val clock: Clock
) {
    fun create(command: CreateOccurrenceCommand): Occurrence {
        val tour = requireTour(command.tourId)
        organizationAccessGuard.requireOperator(command.actorUserId, tour.organizationId)
        val now = clock.instant()
        val startsAtUtc = command.startsAtUtc
        val endsAtUtc = command.endsAtUtc
        requireValidOccurrenceWindow(startsAtUtc, endsAtUtc)
        val occurrence = Occurrence.create(
            organizationId = tour.organizationId,
            tourId = requireNotNull(tour.id),
            instructorProfileId = requireInstructorProfileId(tour.organizationId, command.instructorProfileId),
            capacity = requireValidOccurrenceCapacity(command.capacity),
            startsAtUtc = startsAtUtc,
            endsAtUtc = endsAtUtc,
            timezone = requireValidTimezone(command.timezone),
            unitPrice = requireValidUnitPrice(command.unitPrice),
            currency = requireValidCurrency(command.currency),
            locationText = normalizeOptionalShortText(command.locationText, "locationText", 255),
            meetingPoint = normalizeOptionalShortText(command.meetingPoint, "meetingPoint", 500),
            now = now
        ).copy(id = occurrenceRepository.nextId())
        occurrenceRepository.save(occurrence)
        return occurrence
    }

    fun update(command: UpdateOccurrenceCommand): Occurrence {
        val occurrence = requireOccurrence(command.occurrenceId)
        organizationAccessGuard.requireOperator(command.actorUserId, occurrence.organizationId)
        ensureOccurrenceEditable(occurrence)
        val startsAtUtc = command.startsAtUtc
        val endsAtUtc = command.endsAtUtc
        requireValidOccurrenceWindow(startsAtUtc, endsAtUtc)
        ensureCapacityCanHoldExistingBookings(command.occurrenceId, command.capacity)
        val updated = occurrence.updateAuthoring(
            instructorProfileId = requireInstructorProfileId(occurrence.organizationId, command.instructorProfileId),
            capacity = requireValidOccurrenceCapacity(command.capacity),
            startsAtUtc = startsAtUtc,
            endsAtUtc = endsAtUtc,
            timezone = requireValidTimezone(command.timezone),
            locationText = normalizeOptionalShortText(command.locationText, "locationText", 255),
            meetingPoint = normalizeOptionalShortText(command.meetingPoint, "meetingPoint", 500),
            now = clock.instant()
        )
        occurrenceRepository.save(updated)
        return updated
    }

    fun reschedule(command: RescheduleOccurrenceCommand): Occurrence {
        val occurrence = requireOccurrence(command.occurrenceId)
        organizationAccessGuard.requireOperator(command.actorUserId, occurrence.organizationId)
        ensureOccurrenceEditable(occurrence)
        val now = clock.instant()
        requireValidOccurrenceWindow(command.startsAtUtc, command.endsAtUtc)
        if (!command.startsAtUtc.isAfter(now)) {
            throw DomainException(
                errorCode = ErrorCode.INVALID_STATE_TRANSITION,
                status = 409,
                message = "occurrence can only be rescheduled to a future time"
            )
        }
        if (occurrence.startsAtUtc != null && !occurrence.startsAtUtc.isAfter(now) && hasAnyBookings(command.occurrenceId)) {
            throw DomainException(
                errorCode = ErrorCode.INVALID_STATE_TRANSITION,
                status = 409,
                message = "started occurrence cannot be rescheduled when bookings exist"
            )
        }
        val updated = occurrence.reschedule(
            startsAtUtc = command.startsAtUtc,
            endsAtUtc = command.endsAtUtc,
            timezone = requireValidTimezone(command.timezone),
            locationText = normalizeOptionalShortText(command.locationText, "locationText", 255),
            meetingPoint = normalizeOptionalShortText(command.meetingPoint, "meetingPoint", 500),
            now = now
        )
        occurrenceRepository.save(updated)
        return updated
    }

    private fun requireTour(tourId: Long): Tour =
        tourRepository.findById(tourId) ?: throw DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 404,
            message = "tour $tourId not found"
        )

    private fun requireOccurrence(occurrenceId: Long): Occurrence =
        occurrenceRepository.findById(occurrenceId) ?: throw DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 404,
            message = "occurrence $occurrenceId not found"
        )

    private fun requireInstructorProfileId(organizationId: Long, instructorProfileId: Long?): Long? {
        instructorProfileId ?: return null
        val profile = instructorProfileRepository.findById(instructorProfileId) ?: throw DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 404,
            message = "instructor profile $instructorProfileId not found"
        )
        if (profile.organizationId != organizationId) {
            throw DomainException(
                errorCode = ErrorCode.VALIDATION_ERROR,
                status = 422,
                message = "instructor profile does not belong to the organization"
            )
        }
        if (profile.status != InstructorProfileStatus.ACTIVE) {
            throw DomainException(
                errorCode = ErrorCode.INVALID_STATE_TRANSITION,
                status = 409,
                message = "inactive instructor profile cannot be assigned"
            )
        }
        return instructorProfileId
    }

    private fun ensureOccurrenceEditable(occurrence: Occurrence) {
        if (occurrence.status != OccurrenceStatus.SCHEDULED) {
            throw DomainException(
                errorCode = ErrorCode.INVALID_STATE_TRANSITION,
                status = 409,
                message = "only scheduled occurrences can be edited"
            )
        }
    }

    private fun ensureCapacityCanHoldExistingBookings(occurrenceId: Long, capacity: Int) {
        val occupiedSeats = bookingRepository.findByOccurrenceAndStatuses(
            occurrenceId = occurrenceId,
            statuses = setOf(
                BookingStatus.CONFIRMED,
                BookingStatus.COMPLETED,
                BookingStatus.OFFERED
            )
        ).sumOf { it.partySize }
        if (occupiedSeats > capacity) {
            throw DomainException(
                errorCode = ErrorCode.CAPACITY_EXCEEDED,
                status = 409,
                message = "capacity cannot be lower than occupied seats"
            )
        }
    }

    private fun hasAnyBookings(occurrenceId: Long): Boolean =
        bookingRepository.findByOccurrenceId(occurrenceId).isNotEmpty()
}
