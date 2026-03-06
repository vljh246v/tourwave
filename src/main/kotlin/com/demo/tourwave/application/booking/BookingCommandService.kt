package com.demo.tourwave.application.booking

import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.common.port.IdempotencyDecision
import com.demo.tourwave.application.common.port.IdempotencyStore
import com.demo.tourwave.domain.occurrence.OccurrenceStatus
import java.security.MessageDigest
import java.time.Clock

class BookingCommandService(
    private val bookingRepository: BookingRepository,
    private val occurrenceRepository: OccurrenceRepository,
    private val idempotencyStore: IdempotencyStore,
    private val clock: Clock
) {
    fun createBooking(command: CreateBookingCommand): CreateBookingResult {
        validateCreateRequest(command)

        val pathTemplate = "/occurrences/{occurrenceId}/bookings"
        val requestHash = requestHash(command)

        return when (
            val decision = idempotencyStore.reserveOrReplay(
                actorUserId = command.actorUserId,
                method = "POST",
                pathTemplate = pathTemplate,
                idempotencyKey = command.idempotencyKey,
                requestHash = requestHash
            )
        ) {
            is IdempotencyDecision.Replay -> CreateBookingResult(
                status = decision.status,
                booking = decision.body as BookingCreated
            )

            IdempotencyDecision.Reserved -> {
                val occurrence = occurrenceRepository.getOrCreate(command.occurrenceId)

                if (occurrence.status == OccurrenceStatus.CANCELED) {
                    throw DomainException(
                        errorCode = ErrorCode.OCCURRENCE_ALREADY_CANCELED,
                        status = 409,
                        message = "Occurrence is already canceled",
                        details = mapOf("occurrenceId" to command.occurrenceId)
                    )
                }

                val unavailableSeats = bookingRepository.findByOccurrenceAndStatuses(
                    occurrenceId = command.occurrenceId,
                    statuses = setOf(BookingStatus.CONFIRMED, BookingStatus.OFFERED)
                ).sumOf { it.partySize }

                val availableSeats = (occurrence.capacity - unavailableSeats).coerceAtLeast(0)

                val created = bookingRepository.save(
                    Booking.create(
                        occurrenceId = occurrence.id,
                        organizationId = occurrence.organizationId,
                        leaderUserId = command.actorUserId,
                        partySize = command.partySize,
                        availableSeats = availableSeats
                    ).copy(createdAt = clock.instant())
                )

                val response = BookingCreated(
                    id = requireNotNull(created.id),
                    organizationId = created.organizationId,
                    occurrenceId = created.occurrenceId,
                    userId = created.leaderUserId,
                    partySize = created.partySize,
                    status = created.status,
                    paymentStatus = created.paymentStatus,
                    createdAt = created.createdAt
                )

                idempotencyStore.complete(
                    actorUserId = command.actorUserId,
                    method = "POST",
                    pathTemplate = pathTemplate,
                    idempotencyKey = command.idempotencyKey,
                    status = 201,
                    body = response
                )

                CreateBookingResult(status = 201, booking = response)
            }
        }
    }

    fun mutateBooking(command: MutateBookingCommand): MutateBookingResult {
        val requestHash = requestHashForMutation(command)

        return when (
            val decision = idempotencyStore.reserveOrReplay(
                actorUserId = command.actorUserId,
                method = "POST",
                pathTemplate = command.mutationType.pathTemplate,
                idempotencyKey = command.idempotencyKey,
                requestHash = requestHash
            )
        ) {
            is IdempotencyDecision.Replay -> MutateBookingResult(status = decision.status)
            IdempotencyDecision.Reserved -> {
                val booking = bookingRepository.findById(command.bookingId)
                    ?: throw DomainException(
                        errorCode = ErrorCode.VALIDATION_ERROR,
                        status = 422,
                        message = "Booking not found",
                        details = mapOf("bookingId" to command.bookingId)
                    )

                val mutated = when (command.mutationType) {
                    BookingMutationType.APPROVE -> approveBooking(booking)
                    BookingMutationType.REJECT -> rejectBooking(booking)
                    BookingMutationType.CANCEL -> cancelBooking(booking)
                    BookingMutationType.OFFER_ACCEPT -> acceptOffer(booking, command.actorUserId)
                    BookingMutationType.OFFER_DECLINE -> declineOffer(booking, command.actorUserId)
                }

                bookingRepository.save(mutated)

                idempotencyStore.complete(
                    actorUserId = command.actorUserId,
                    method = "POST",
                    pathTemplate = command.mutationType.pathTemplate,
                    idempotencyKey = command.idempotencyKey,
                    status = 204,
                    body = mapOf("ok" to true)
                )

                MutateBookingResult(status = 204)
            }
        }
    }

    private fun validateCreateRequest(command: CreateBookingCommand) {
        if (command.partySize !in 1..50) {
            throw DomainException(
                errorCode = ErrorCode.PARTY_SIZE_OUT_OF_RANGE,
                status = 422,
                message = "partySize must be between 1 and 50",
                details = mapOf("partySize" to command.partySize)
            )
        }
    }

    private fun requestHash(command: CreateBookingCommand): String {
        val raw = "${command.occurrenceId}|${command.partySize}|${command.noteToOperator ?: ""}"
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun requestHashForMutation(command: MutateBookingCommand): String {
        val raw = "${command.bookingId}|${command.mutationType.name}"
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun approveBooking(booking: Booking): Booking {
        if (booking.status.isTerminal()) {
            throw DomainException(
                errorCode = ErrorCode.BOOKING_TERMINAL_STATE,
                status = 409,
                message = "Booking is in terminal state",
                details = mapOf("bookingId" to booking.id, "status" to booking.status)
            )
        }
        if (booking.status != BookingStatus.REQUESTED) {
            throw DomainException(
                errorCode = ErrorCode.INVALID_STATE_TRANSITION,
                status = 409,
                message = "Only REQUESTED booking can be approved",
                details = mapOf("bookingId" to booking.id, "status" to booking.status)
            )
        }

        ensureCapacityAvailable(
            occurrenceId = booking.occurrenceId,
            excludeBookingId = booking.id,
            additionalSeats = booking.partySize
        )

        return booking.approve()
    }

    private fun rejectBooking(booking: Booking): Booking {
        if (booking.status.isTerminal()) {
            throw DomainException(
                errorCode = ErrorCode.BOOKING_TERMINAL_STATE,
                status = 409,
                message = "Booking is in terminal state",
                details = mapOf("bookingId" to booking.id, "status" to booking.status)
            )
        }
        if (booking.status != BookingStatus.REQUESTED) {
            throw DomainException(
                errorCode = ErrorCode.INVALID_STATE_TRANSITION,
                status = 409,
                message = "Only REQUESTED booking can be rejected",
                details = mapOf("bookingId" to booking.id, "status" to booking.status)
            )
        }
        return booking.reject()
    }

    private fun cancelBooking(booking: Booking): Booking {
        if (booking.status.isTerminal()) {
            throw DomainException(
                errorCode = ErrorCode.BOOKING_TERMINAL_STATE,
                status = 409,
                message = "Booking is in terminal state",
                details = mapOf("bookingId" to booking.id, "status" to booking.status)
            )
        }

        val shouldRefund = booking.paymentStatus == PaymentStatus.AUTHORIZED || booking.paymentStatus == PaymentStatus.PAID
        return booking.cancel(refund = shouldRefund)
    }

    private fun acceptOffer(booking: Booking, actorUserId: Long): Booking {
        ensureLeader(actorUserId, booking)

        if (booking.status != BookingStatus.OFFERED) {
            val code = if (booking.status.isTerminal()) ErrorCode.BOOKING_TERMINAL_STATE else ErrorCode.OFFER_NOT_ACTIVE
            throw DomainException(
                errorCode = code,
                status = 409,
                message = "Offer is not active",
                details = mapOf("bookingId" to booking.id, "status" to booking.status)
            )
        }

        val offerExpiresAtUtc = booking.offerExpiresAtUtc
            ?: throw DomainException(
                errorCode = ErrorCode.OFFER_NOT_ACTIVE,
                status = 409,
                message = "Offer expiration is missing",
                details = mapOf("bookingId" to booking.id)
            )

        val now = clock.instant()
        if (now.isAfter(offerExpiresAtUtc)) {
            throw DomainException(
                errorCode = ErrorCode.OFFER_EXPIRED,
                status = 409,
                message = "Offer is expired",
                details = mapOf("bookingId" to booking.id, "offerExpiresAtUtc" to offerExpiresAtUtc)
            )
        }

        ensureCapacityAvailable(
            occurrenceId = booking.occurrenceId,
            excludeBookingId = booking.id,
            additionalSeats = booking.partySize
        )

        return booking.acceptOffer(now)
    }

    private fun declineOffer(booking: Booking, actorUserId: Long): Booking {
        ensureLeader(actorUserId, booking)

        if (booking.status != BookingStatus.OFFERED) {
            val code = if (booking.status.isTerminal()) ErrorCode.BOOKING_TERMINAL_STATE else ErrorCode.OFFER_NOT_ACTIVE
            throw DomainException(
                errorCode = code,
                status = 409,
                message = "Offer is not active",
                details = mapOf("bookingId" to booking.id, "status" to booking.status)
            )
        }

        val offerExpiresAtUtc = booking.offerExpiresAtUtc
            ?: throw DomainException(
                errorCode = ErrorCode.OFFER_NOT_ACTIVE,
                status = 409,
                message = "Offer expiration is missing",
                details = mapOf("bookingId" to booking.id)
            )

        val now = clock.instant()
        if (now.isAfter(offerExpiresAtUtc)) {
            throw DomainException(
                errorCode = ErrorCode.OFFER_EXPIRED,
                status = 409,
                message = "Offer is expired",
                details = mapOf("bookingId" to booking.id, "offerExpiresAtUtc" to offerExpiresAtUtc)
            )
        }

        return booking.declineOffer(now)
    }

    private fun ensureLeader(actorUserId: Long, booking: Booking) {
        if (actorUserId != booking.leaderUserId) {
            throw DomainException(
                errorCode = ErrorCode.VALIDATION_ERROR,
                status = 422,
                message = "Only booking leader can perform this action",
                details = mapOf("bookingId" to booking.id, "actorUserId" to actorUserId)
            )
        }
    }

    private fun ensureCapacityAvailable(
        occurrenceId: Long,
        excludeBookingId: Long?,
        additionalSeats: Int
    ) {
        val occurrence = occurrenceRepository.getOrCreate(occurrenceId)

        val occupiedSeats = bookingRepository.findByOccurrenceAndStatuses(
            occurrenceId = occurrenceId,
            statuses = setOf(BookingStatus.CONFIRMED, BookingStatus.OFFERED)
        )
            .filterNot { excludeBookingId != null && it.id == excludeBookingId }
            .sumOf { it.partySize }

        if (occupiedSeats + additionalSeats > occurrence.capacity) {
            throw DomainException(
                errorCode = ErrorCode.CAPACITY_EXCEEDED,
                status = 409,
                message = "Seat allocation exceeds occurrence capacity",
                details = mapOf(
                    "occurrenceId" to occurrenceId,
                    "capacity" to occurrence.capacity,
                    "occupiedSeats" to occupiedSeats,
                    "requestedSeats" to additionalSeats
                )
            )
        }
    }
}
