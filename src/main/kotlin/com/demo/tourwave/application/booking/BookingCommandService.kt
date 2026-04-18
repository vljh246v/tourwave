package com.demo.tourwave.application.booking

import com.demo.tourwave.application.booking.port.BookingRepository
import com.demo.tourwave.application.booking.port.OccurrenceRepository
import com.demo.tourwave.application.common.TimeWindowPolicyService
import com.demo.tourwave.application.common.port.AuditEventCommand
import com.demo.tourwave.application.common.port.AuditEventPort
import com.demo.tourwave.application.common.port.IdempotencyDecision
import com.demo.tourwave.application.common.port.IdempotencyStore
import com.demo.tourwave.application.participant.port.BookingParticipantRepository
import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.booking.RefundPolicyAction
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.demo.tourwave.domain.occurrence.Occurrence
import com.demo.tourwave.domain.occurrence.OccurrenceStatus
import com.demo.tourwave.domain.participant.BookingParticipant
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Clock

@Transactional
class BookingCommandService(
    private val bookingRepository: BookingRepository,
    private val occurrenceRepository: OccurrenceRepository,
    private val bookingParticipantRepository: BookingParticipantRepository,
    private val idempotencyStore: IdempotencyStore,
    private val auditEventPort: AuditEventPort,
    private val paymentLedgerService: PaymentLedgerService,
    private val timeWindowPolicyService: TimeWindowPolicyService,
    private val clock: Clock,
) {
    companion object {
        private const val OFFER_WINDOW_SECONDS = 24 * 60 * 60L
    }

    fun createBooking(command: CreateBookingCommand): CreateBookingResult {
        validateCreateRequest(command)

        val pathTemplate = "/occurrences/{occurrenceId}/bookings"
        val requestHash = requestHash(command)

        return when (
            val decision =
                idempotencyStore.reserveOrReplay(
                    actorUserId = command.actorUserId,
                    method = "POST",
                    pathTemplate = pathTemplate,
                    idempotencyKey = command.idempotencyKey,
                    requestHash = requestHash,
                )
        ) {
            is IdempotencyDecision.Replay ->
                CreateBookingResult(
                    status = decision.status,
                    booking = decision.body as BookingCreated,
                )

            IdempotencyDecision.Reserved -> {
                occurrenceRepository.lock(command.occurrenceId)
                val occurrence = occurrenceRepository.getOrCreate(command.occurrenceId)

                if (occurrence.status == OccurrenceStatus.FINISHED) {
                    throw DomainException(
                        errorCode = ErrorCode.INVALID_STATE_TRANSITION,
                        status = 409,
                        message = "Occurrence is already finished",
                        details = mapOf("occurrenceId" to command.occurrenceId, "status" to occurrence.status),
                    )
                }

                if (occurrence.status == OccurrenceStatus.CANCELED) {
                    throw DomainException(
                        errorCode = ErrorCode.OCCURRENCE_ALREADY_CANCELED,
                        status = 409,
                        message = "Occurrence is already canceled",
                        details = mapOf("occurrenceId" to command.occurrenceId),
                    )
                }

                val availableSeats =
                    availableSeatsForOccurrence(
                        occurrenceId = command.occurrenceId,
                        excludeBookingId = null,
                        now = clock.instant(),
                    )

                val created =
                    bookingRepository.save(
                        Booking.create(
                            occurrenceId = occurrence.id,
                            organizationId = occurrence.organizationId,
                            leaderUserId = command.actorUserId,
                            partySize = command.partySize,
                            availableSeats = availableSeats,
                        ).copy(createdAt = clock.instant()),
                    )

                bookingParticipantRepository.save(
                    BookingParticipant.leader(
                        bookingId = requireNotNull(created.id),
                        userId = created.leaderUserId,
                        createdAt = created.createdAt,
                    ),
                )

                paymentLedgerService.initialize(
                    booking = created,
                    occurrence = occurrence,
                    actorUserId = command.actorUserId,
                )

                val response =
                    BookingCreated(
                        id = requireNotNull(created.id),
                        organizationId = created.organizationId,
                        occurrenceId = created.occurrenceId,
                        userId = created.leaderUserId,
                        partySize = created.partySize,
                        status = created.status,
                        paymentStatus = created.paymentStatus,
                        createdAt = created.createdAt,
                    )

                idempotencyStore.complete(
                    actorUserId = command.actorUserId,
                    method = "POST",
                    pathTemplate = pathTemplate,
                    idempotencyKey = command.idempotencyKey,
                    status = 201,
                    body = response,
                )

                auditEventPort.append(
                    AuditEventCommand(
                        actor = "USER:${command.actorUserId}",
                        action = "BOOKING_CREATED",
                        resourceType = "BOOKING",
                        resourceId = response.id,
                        occurredAtUtc = clock.instant(),
                        requestId = command.requestId,
                    ),
                )

                CreateBookingResult(status = 201, booking = response)
            }
        }
    }

    fun finishOccurrence(command: FinishOccurrenceCommand): FinishOccurrenceResult {
        val pathTemplate = "/occurrences/{occurrenceId}/finish"
        val requestHash = hash("${command.occurrenceId}|finish")

        return when (
            val decision =
                idempotencyStore.reserveOrReplay(
                    actorUserId = command.actorUserId,
                    method = "POST",
                    pathTemplate = pathTemplate,
                    idempotencyKey = command.idempotencyKey,
                    requestHash = requestHash,
                )
        ) {
            is IdempotencyDecision.Replay -> FinishOccurrenceResult(status = decision.status)
            IdempotencyDecision.Reserved -> {
                occurrenceRepository.lock(command.occurrenceId)
                val occurrence = occurrenceRepository.getOrCreate(command.occurrenceId)
                when (occurrence.status) {
                    OccurrenceStatus.CANCELED -> throw DomainException(
                        errorCode = ErrorCode.OCCURRENCE_ALREADY_CANCELED,
                        status = 409,
                        message = "Occurrence is already canceled",
                        details = mapOf("occurrenceId" to command.occurrenceId),
                    )

                    OccurrenceStatus.FINISHED -> throw DomainException(
                        errorCode = ErrorCode.INVALID_STATE_TRANSITION,
                        status = 409,
                        message = "Occurrence is already finished",
                        details = mapOf("occurrenceId" to command.occurrenceId, "status" to occurrence.status),
                    )

                    OccurrenceStatus.SCHEDULED -> {
                        occurrenceRepository.save(occurrence.copy(status = OccurrenceStatus.FINISHED))
                    }
                }

                idempotencyStore.complete(
                    actorUserId = command.actorUserId,
                    method = "POST",
                    pathTemplate = pathTemplate,
                    idempotencyKey = command.idempotencyKey,
                    status = 204,
                    body = mapOf("ok" to true),
                )

                auditEventPort.append(
                    AuditEventCommand(
                        actor = "USER:${command.actorUserId}",
                        action = "OCCURRENCE_FINISHED",
                        resourceType = "OCCURRENCE",
                        resourceId = occurrence.id,
                        occurredAtUtc = clock.instant(),
                        requestId = command.requestId,
                    ),
                )

                FinishOccurrenceResult(status = 204)
            }
        }
    }

    fun cancelOccurrence(command: CancelOccurrenceCommand): CancelOccurrenceResult {
        val pathTemplate = "/occurrences/{occurrenceId}/cancel"
        val requestHash = hash("${command.occurrenceId}|cancel")

        return when (
            val decision =
                idempotencyStore.reserveOrReplay(
                    actorUserId = command.actorUserId,
                    method = "POST",
                    pathTemplate = pathTemplate,
                    idempotencyKey = command.idempotencyKey,
                    requestHash = requestHash,
                )
        ) {
            is IdempotencyDecision.Replay -> CancelOccurrenceResult(status = decision.status)
            IdempotencyDecision.Reserved -> {
                occurrenceRepository.lock(command.occurrenceId)
                val occurrence = occurrenceRepository.getOrCreate(command.occurrenceId)

                if (occurrence.status != OccurrenceStatus.CANCELED) {
                    occurrenceRepository.save(occurrence.copy(status = OccurrenceStatus.CANCELED))
                }

                val nonTerminalStatuses =
                    setOf(
                        BookingStatus.REQUESTED,
                        BookingStatus.WAITLISTED,
                        BookingStatus.OFFERED,
                        BookingStatus.CONFIRMED,
                    )
                val bookingsToCancel = bookingRepository.findByOccurrenceAndStatuses(command.occurrenceId, nonTerminalStatuses)

                bookingsToCancel.forEach { booking ->
                    val canceled = bookingRepository.save(booking.cancel())
                    val refunded =
                        paymentLedgerService.applyRefundPolicy(
                            booking = canceled,
                            occurrence = occurrence,
                            action = RefundPolicyAction.OCCURRENCE_CANCEL,
                            actorUserId = command.actorUserId,
                        )
                    val settled = bookingRepository.save(refunded)
                    cancelParticipants(bookingId = requireNotNull(settled.id), canceledAt = clock.instant())
                    appendBookingStatusAudit(
                        actorUserId = command.actorUserId,
                        booking = settled,
                        action = "BOOKING_CANCELED_BY_OCCURRENCE",
                        requestId = command.requestId,
                    )
                }

                idempotencyStore.complete(
                    actorUserId = command.actorUserId,
                    method = "POST",
                    pathTemplate = pathTemplate,
                    idempotencyKey = command.idempotencyKey,
                    status = 204,
                    body = mapOf("ok" to true),
                )

                auditEventPort.append(
                    AuditEventCommand(
                        actor = "USER:${command.actorUserId}",
                        action = "OCCURRENCE_CANCELED",
                        resourceType = "OCCURRENCE",
                        resourceId = occurrence.id,
                        occurredAtUtc = clock.instant(),
                        requestId = command.requestId,
                    ),
                )

                CancelOccurrenceResult(status = 204)
            }
        }
    }

    fun mutateBooking(command: MutateBookingCommand): MutateBookingResult {
        val requestHash = requestHashForMutation(command)

        return when (
            val decision =
                idempotencyStore.reserveOrReplay(
                    actorUserId = command.actorUserId,
                    method = command.mutationType.httpMethod,
                    pathTemplate = command.mutationType.pathTemplate,
                    idempotencyKey = command.idempotencyKey,
                    requestHash = requestHash,
                )
        ) {
            is IdempotencyDecision.Replay -> {
                val replayedBooking = if (decision.status == 200) decision.body as Booking else null
                MutateBookingResult(status = decision.status, booking = replayedBooking)
            }
            IdempotencyDecision.Reserved -> {
                val booking =
                    bookingRepository.findById(command.bookingId)
                        ?: throw DomainException(
                            errorCode = ErrorCode.VALIDATION_ERROR,
                            status = 422,
                            message = "Booking not found",
                            details = mapOf("bookingId" to command.bookingId),
                        )
                occurrenceRepository.lock(booking.occurrenceId)

                val mutated =
                    when (command.mutationType) {
                        BookingMutationType.APPROVE -> approveBooking(booking)
                        BookingMutationType.REJECT -> rejectBooking(booking)
                        BookingMutationType.CANCEL -> cancelBooking(booking)
                        BookingMutationType.OFFER_ACCEPT -> acceptOffer(booking, command.actorUserId, command.requestId)
                        BookingMutationType.OFFER_DECLINE -> declineOffer(booking, command.actorUserId, command.requestId)
                        BookingMutationType.PARTY_SIZE_PATCH -> patchPartySize(booking, command.actorUserId, command.partySize)
                        BookingMutationType.COMPLETE -> completeBooking(booking)
                    }

                var persisted = bookingRepository.save(mutated)
                persisted =
                    applyPaymentLifecycle(
                        before = booking,
                        after = persisted,
                        mutationType = command.mutationType,
                        actorUserId = command.actorUserId,
                        requestId = command.requestId,
                    )
                if (persisted != mutated) {
                    persisted = bookingRepository.save(persisted)
                }
                if (command.mutationType == BookingMutationType.CANCEL) {
                    cancelParticipants(
                        bookingId = requireNotNull(persisted.id),
                        canceledAt = clock.instant(),
                    )
                }
                promoteWaitlistIfSeatReleased(command, booking, persisted)

                idempotencyStore.complete(
                    actorUserId = command.actorUserId,
                    method = command.mutationType.httpMethod,
                    pathTemplate = command.mutationType.pathTemplate,
                    idempotencyKey = command.idempotencyKey,
                    status = if (command.mutationType == BookingMutationType.PARTY_SIZE_PATCH) 200 else 204,
                    body = if (command.mutationType == BookingMutationType.PARTY_SIZE_PATCH) persisted else mapOf("ok" to true),
                )

                appendBookingMutationAudit(command, persisted)

                val status = if (command.mutationType == BookingMutationType.PARTY_SIZE_PATCH) 200 else 204
                MutateBookingResult(status = status, booking = if (status == 200) persisted else null)
            }
        }
    }

    private fun validateCreateRequest(command: CreateBookingCommand) {
        if (command.partySize !in 1..50) {
            throw DomainException(
                errorCode = ErrorCode.PARTY_SIZE_OUT_OF_RANGE,
                status = 422,
                message = "partySize must be between 1 and 50",
                details = mapOf("partySize" to command.partySize),
            )
        }
    }

    private fun requestHash(command: CreateBookingCommand): String {
        val raw = "${command.occurrenceId}|${command.partySize}|${command.noteToOperator ?: ""}"
        return hash(raw)
    }

    private fun requestHashForMutation(command: MutateBookingCommand): String {
        val raw = "${command.bookingId}|${command.mutationType.name}|${command.partySize ?: ""}"
        return hash(raw)
    }

    private fun hash(raw: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun approveBooking(booking: Booking): Booking {
        if (booking.status.isTerminal()) {
            throw DomainException(
                errorCode = ErrorCode.BOOKING_TERMINAL_STATE,
                status = 409,
                message = "Booking is in terminal state",
                details = mapOf("bookingId" to booking.id, "status" to booking.status),
            )
        }
        if (booking.status != BookingStatus.REQUESTED) {
            throw DomainException(
                errorCode = ErrorCode.INVALID_STATE_TRANSITION,
                status = 409,
                message = "Only REQUESTED booking can be approved",
                details = mapOf("bookingId" to booking.id, "status" to booking.status),
            )
        }

        ensureCapacityAvailable(
            occurrenceId = booking.occurrenceId,
            excludeBookingId = booking.id,
            additionalSeats = booking.partySize,
        )

        return booking.approve()
    }

    private fun rejectBooking(booking: Booking): Booking {
        if (booking.status.isTerminal()) {
            throw DomainException(
                errorCode = ErrorCode.BOOKING_TERMINAL_STATE,
                status = 409,
                message = "Booking is in terminal state",
                details = mapOf("bookingId" to booking.id, "status" to booking.status),
            )
        }
        if (booking.status != BookingStatus.REQUESTED) {
            throw DomainException(
                errorCode = ErrorCode.INVALID_STATE_TRANSITION,
                status = 409,
                message = "Only REQUESTED booking can be rejected",
                details = mapOf("bookingId" to booking.id, "status" to booking.status),
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
                details = mapOf("bookingId" to booking.id, "status" to booking.status),
            )
        }

        return booking.cancel()
    }

    private fun completeBooking(booking: Booking): Booking {
        if (booking.status.isTerminal()) {
            throw DomainException(
                errorCode = ErrorCode.BOOKING_TERMINAL_STATE,
                status = 409,
                message = "Booking is in terminal state",
                details = mapOf("bookingId" to booking.id, "status" to booking.status),
            )
        }

        if (booking.status != BookingStatus.CONFIRMED) {
            throw DomainException(
                errorCode = ErrorCode.INVALID_STATE_TRANSITION,
                status = 409,
                message = "Only CONFIRMED booking can be completed",
                details = mapOf("bookingId" to booking.id, "status" to booking.status),
            )
        }

        return booking.complete()
    }

    private fun acceptOffer(
        booking: Booking,
        actorUserId: Long,
        requestId: String?,
    ): Booking {
        ensureLeader(actorUserId, booking)
        ensureOccurrenceNotCanceled(booking.occurrenceId)

        if (booking.status != BookingStatus.OFFERED) {
            val code = if (booking.status.isTerminal()) ErrorCode.BOOKING_TERMINAL_STATE else ErrorCode.OFFER_NOT_ACTIVE
            throw DomainException(
                errorCode = code,
                status = 409,
                message = "Offer is not active",
                details = mapOf("bookingId" to booking.id, "status" to booking.status),
            )
        }

        val offerExpiresAtUtc =
            booking.offerExpiresAtUtc
                ?: throw DomainException(
                    errorCode = ErrorCode.OFFER_NOT_ACTIVE,
                    status = 409,
                    message = "Offer expiration is missing",
                    details = mapOf("bookingId" to booking.id),
                )

        val now = clock.instant()
        if (timeWindowPolicyService.isOfferExpired(now, offerExpiresAtUtc)) {
            val expired = bookingRepository.save(booking.expireOffer())
            val settled =
                bookingRepository.save(
                    paymentLedgerService.applyRefundPolicy(
                        booking = expired,
                        occurrence = occurrenceRepository.getOrCreate(booking.occurrenceId),
                        action = RefundPolicyAction.OFFER_EXPIRED,
                        actorUserId = actorUserId,
                    ),
                )
            appendBookingStatusAudit(
                actorUserId = actorUserId,
                booking = settled,
                action = "OFFER_EXPIRED",
                requestId = requestId,
            )
            promoteWaitlist(
                occurrenceId = booking.occurrenceId,
                actorUserId = actorUserId,
                requestId = requestId,
            )
            throw DomainException(
                errorCode = ErrorCode.OFFER_EXPIRED,
                status = 409,
                message = "Offer is expired",
                details = mapOf("bookingId" to booking.id, "offerExpiresAtUtc" to offerExpiresAtUtc),
            )
        }

        ensureCapacityAvailable(
            occurrenceId = booking.occurrenceId,
            excludeBookingId = booking.id,
            additionalSeats = booking.partySize,
        )

        return booking.acceptOffer(now)
    }

    private fun declineOffer(
        booking: Booking,
        actorUserId: Long,
        requestId: String?,
    ): Booking {
        ensureLeader(actorUserId, booking)
        ensureOccurrenceNotCanceled(booking.occurrenceId)

        if (booking.status != BookingStatus.OFFERED) {
            val code = if (booking.status.isTerminal()) ErrorCode.BOOKING_TERMINAL_STATE else ErrorCode.OFFER_NOT_ACTIVE
            throw DomainException(
                errorCode = code,
                status = 409,
                message = "Offer is not active",
                details = mapOf("bookingId" to booking.id, "status" to booking.status),
            )
        }

        val offerExpiresAtUtc =
            booking.offerExpiresAtUtc
                ?: throw DomainException(
                    errorCode = ErrorCode.OFFER_NOT_ACTIVE,
                    status = 409,
                    message = "Offer expiration is missing",
                    details = mapOf("bookingId" to booking.id),
                )

        val now = clock.instant()
        if (now.isAfter(offerExpiresAtUtc)) {
            val expired = bookingRepository.save(booking.expireOffer())
            val settled =
                bookingRepository.save(
                    paymentLedgerService.applyRefundPolicy(
                        booking = expired,
                        occurrence = occurrenceRepository.getOrCreate(booking.occurrenceId),
                        action = RefundPolicyAction.OFFER_EXPIRED,
                        actorUserId = actorUserId,
                    ),
                )
            appendBookingStatusAudit(
                actorUserId = actorUserId,
                booking = settled,
                action = "OFFER_EXPIRED",
                requestId = requestId,
            )
            promoteWaitlist(
                occurrenceId = booking.occurrenceId,
                actorUserId = actorUserId,
                requestId = requestId,
            )
            throw DomainException(
                errorCode = ErrorCode.OFFER_EXPIRED,
                status = 409,
                message = "Offer is expired",
                details = mapOf("bookingId" to booking.id, "offerExpiresAtUtc" to offerExpiresAtUtc),
            )
        }

        return booking.declineOffer(now)
    }

    private fun ensureLeader(
        actorUserId: Long,
        booking: Booking,
    ) {
        if (actorUserId != booking.leaderUserId) {
            throw DomainException(
                errorCode = ErrorCode.VALIDATION_ERROR,
                status = 422,
                message = "Only booking leader can perform this action",
                details = mapOf("bookingId" to booking.id, "actorUserId" to actorUserId),
            )
        }
    }

    private fun ensureCapacityAvailable(
        occurrenceId: Long,
        excludeBookingId: Long?,
        additionalSeats: Int,
    ) {
        val occurrence = occurrenceRepository.getOrCreate(occurrenceId)
        ensureOccurrenceCanAllocate(occurrence)

        val occupiedSeats =
            occupiedSeatsForOccurrence(
                occurrenceId = occurrenceId,
                excludeBookingId = excludeBookingId,
                now = clock.instant(),
            )

        if (occupiedSeats + additionalSeats > occurrence.capacity) {
            throw DomainException(
                errorCode = ErrorCode.CAPACITY_EXCEEDED,
                status = 409,
                message = "Seat allocation exceeds occurrence capacity",
                details =
                    mapOf(
                        "occurrenceId" to occurrenceId,
                        "capacity" to occurrence.capacity,
                        "occupiedSeats" to occupiedSeats,
                        "requestedSeats" to additionalSeats,
                    ),
            )
        }
    }

    private fun ensureOccurrenceCanAllocate(occurrence: Occurrence) {
        if (occurrence.status == OccurrenceStatus.CANCELED) {
            throw DomainException(
                errorCode = ErrorCode.OCCURRENCE_ALREADY_CANCELED,
                status = 409,
                message = "Occurrence is already canceled",
                details = mapOf("occurrenceId" to occurrence.id),
            )
        }
    }

    private fun ensureOccurrenceNotCanceled(occurrenceId: Long) {
        val occurrence = occurrenceRepository.getOrCreate(occurrenceId)
        ensureOccurrenceCanAllocate(occurrence)
    }

    private fun patchPartySize(
        booking: Booking,
        actorUserId: Long,
        partySize: Int?,
    ): Booking {
        ensureLeader(actorUserId, booking)

        if (booking.status != BookingStatus.CONFIRMED) {
            throw DomainException(
                errorCode = ErrorCode.INVALID_STATE_TRANSITION,
                status = 409,
                message = "Only CONFIRMED booking can update party size",
                details = mapOf("bookingId" to booking.id, "status" to booking.status),
            )
        }

        val targetPartySize =
            partySize ?: throw DomainException(
                errorCode = ErrorCode.REQUIRED_FIELD_MISSING,
                status = 422,
                message = "partySize is required",
                details = mapOf("field" to "partySize"),
            )

        if (targetPartySize > booking.partySize) {
            throw DomainException(
                errorCode = ErrorCode.PARTY_SIZE_INCREASE_NOT_ALLOWED,
                status = 422,
                message = "partySize can only be decreased",
                details =
                    mapOf(
                        "bookingId" to booking.id,
                        "currentPartySize" to booking.partySize,
                        "requestedPartySize" to targetPartySize,
                    ),
            )
        }

        val activeParticipants = bookingParticipantRepository.findByBookingId(requireNotNull(booking.id)).count { it.isActive() }
        if (targetPartySize < activeParticipants) {
            throw DomainException(
                errorCode = ErrorCode.VALIDATION_ERROR,
                status = 422,
                message = "partySize cannot be lower than active participant count",
                details =
                    mapOf(
                        "bookingId" to booking.id,
                        "activeParticipantCount" to activeParticipants,
                        "requestedPartySize" to targetPartySize,
                    ),
            )
        }

        return booking.decreasePartySize(targetPartySize)
    }

    private fun appendBookingMutationAudit(
        command: MutateBookingCommand,
        booking: Booking,
    ) {
        val action =
            when (command.mutationType) {
                BookingMutationType.APPROVE -> "BOOKING_APPROVED"
                BookingMutationType.REJECT -> "BOOKING_REJECTED"
                BookingMutationType.CANCEL -> "BOOKING_CANCELED"
                BookingMutationType.OFFER_ACCEPT -> "OFFER_ACCEPTED"
                BookingMutationType.OFFER_DECLINE -> "OFFER_DECLINED"
                BookingMutationType.PARTY_SIZE_PATCH -> "PARTY_SIZE_CHANGED"
                BookingMutationType.COMPLETE -> "BOOKING_COMPLETED"
            }

        appendBookingStatusAudit(
            actorUserId = command.actorUserId,
            booking = booking,
            action = action,
            requestId = command.requestId,
        )
    }

    private fun applyPaymentLifecycle(
        before: Booking,
        after: Booking,
        mutationType: BookingMutationType,
        actorUserId: Long,
        requestId: String?,
    ): Booking {
        val occurrence =
            when (mutationType) {
                BookingMutationType.APPROVE,
                BookingMutationType.REJECT,
                BookingMutationType.CANCEL,
                BookingMutationType.OFFER_ACCEPT,
                BookingMutationType.OFFER_DECLINE,
                -> occurrenceRepository.getOrCreate(after.occurrenceId)

                BookingMutationType.PARTY_SIZE_PATCH,
                BookingMutationType.COMPLETE,
                -> null
            }

        val settled =
            when (mutationType) {
                BookingMutationType.APPROVE,
                BookingMutationType.OFFER_ACCEPT,
                -> paymentLedgerService.capture(after, actorUserId)

                BookingMutationType.REJECT ->
                    paymentLedgerService.applyRefundPolicy(
                        booking = after,
                        occurrence = requireNotNull(occurrence),
                        action = RefundPolicyAction.BOOKING_REJECTED,
                        actorUserId = actorUserId,
                    )

                BookingMutationType.CANCEL ->
                    paymentLedgerService.applyRefundPolicy(
                        booking = after,
                        occurrence = requireNotNull(occurrence),
                        action = RefundPolicyAction.LEADER_CANCEL,
                        actorUserId = actorUserId,
                    )

                BookingMutationType.OFFER_DECLINE ->
                    paymentLedgerService.applyRefundPolicy(
                        booking = after,
                        occurrence = requireNotNull(occurrence),
                        action = RefundPolicyAction.OFFER_DECLINED,
                        actorUserId = actorUserId,
                    )

                BookingMutationType.PARTY_SIZE_PATCH,
                BookingMutationType.COMPLETE,
                -> after
            }

        if (mutationType == BookingMutationType.CANCEL && before != settled && settled.paymentStatus == PaymentStatus.REFUND_PENDING) {
            auditEventPort.append(
                AuditEventCommand(
                    actor = "USER:$actorUserId",
                    action = "REFUND_PENDING",
                    resourceType = "BOOKING",
                    resourceId = requireNotNull(after.id),
                    occurredAtUtc = clock.instant(),
                    requestId = requestId,
                    reasonCode = "REFUND_EXECUTION_PENDING",
                    beforeJson = bookingSnapshot(before),
                    afterJson = bookingSnapshot(settled),
                ),
            )
        }

        return settled
    }

    private fun appendBookingStatusAudit(
        actorUserId: Long,
        booking: Booking,
        action: String,
        requestId: String?,
    ) {
        auditEventPort.append(
            AuditEventCommand(
                actor = "USER:$actorUserId",
                action = action,
                resourceType = "BOOKING",
                resourceId = requireNotNull(booking.id),
                occurredAtUtc = clock.instant(),
                requestId = requestId,
                reasonCode = action,
                afterJson = bookingSnapshot(booking),
            ),
        )
    }

    private fun promoteWaitlistIfSeatReleased(
        command: MutateBookingCommand,
        before: Booking,
        after: Booking,
    ) {
        val shouldPromote =
            when (command.mutationType) {
                BookingMutationType.CANCEL -> isSeatHolding(before, clock.instant())
                BookingMutationType.OFFER_DECLINE -> before.status == BookingStatus.OFFERED
                BookingMutationType.PARTY_SIZE_PATCH -> after.partySize < before.partySize
                BookingMutationType.APPROVE,
                BookingMutationType.REJECT,
                BookingMutationType.OFFER_ACCEPT,
                BookingMutationType.COMPLETE,
                -> false
            }

        if (!shouldPromote) {
            return
        }

        promoteWaitlist(
            occurrenceId = after.occurrenceId,
            actorUserId = command.actorUserId,
            requestId = command.requestId,
        )
    }

    private fun promoteWaitlist(
        occurrenceId: Long,
        actorUserId: Long,
        requestId: String?,
    ) {
        val occurrence = occurrenceRepository.getOrCreate(occurrenceId)
        if (occurrence.status == OccurrenceStatus.CANCELED) {
            return
        }

        val now = clock.instant()
        var availableSeats =
            availableSeatsForOccurrence(
                occurrenceId = occurrenceId,
                excludeBookingId = null,
                now = now,
            )

        if (availableSeats <= 0) {
            return
        }

        val waitlistedBookings = bookingRepository.findWaitlistedByOccurrenceOrdered(occurrenceId)

        waitlistedBookings.forEach { waitlisted ->
            if (waitlisted.partySize <= availableSeats) {
                val promoted =
                    bookingRepository.save(
                        waitlisted.offer(now.plusSeconds(OFFER_WINDOW_SECONDS)),
                    )
                availableSeats -= promoted.partySize

                appendBookingStatusAudit(
                    actorUserId = actorUserId,
                    booking = promoted,
                    action = "WAITLIST_PROMOTED_TO_OFFER",
                    requestId = requestId,
                )
            }
        }
    }

    private fun availableSeatsForOccurrence(
        occurrenceId: Long,
        excludeBookingId: Long?,
        now: java.time.Instant,
    ): Int {
        val occurrence = occurrenceRepository.getOrCreate(occurrenceId)
        val occupiedSeats = occupiedSeatsForOccurrence(occurrenceId, excludeBookingId, now)
        return (occurrence.capacity - occupiedSeats).coerceAtLeast(0)
    }

    private fun occupiedSeatsForOccurrence(
        occurrenceId: Long,
        excludeBookingId: Long?,
        now: java.time.Instant,
    ): Int {
        return bookingRepository.findByOccurrenceAndStatuses(
            occurrenceId = occurrenceId,
            statuses = setOf(BookingStatus.CONFIRMED, BookingStatus.OFFERED),
        )
            .filterNot { excludeBookingId != null && it.id == excludeBookingId }
            .filter { isSeatHolding(it, now) }
            .sumOf { it.partySize }
    }

    private fun isSeatHolding(
        booking: Booking,
        now: java.time.Instant,
    ): Boolean {
        return when (booking.status) {
            BookingStatus.CONFIRMED -> true
            BookingStatus.OFFERED -> {
                val expiresAt = booking.offerExpiresAtUtc ?: return true
                !timeWindowPolicyService.isOfferExpired(now, expiresAt)
            }

            else -> false
        }
    }

    private fun cancelParticipants(
        bookingId: Long,
        canceledAt: java.time.Instant,
    ) {
        bookingParticipantRepository.findByBookingId(bookingId)
            .forEach { participant ->
                bookingParticipantRepository.save(participant.cancel(canceledAt))
            }
    }

    private fun bookingSnapshot(booking: Booking): Map<String, Any?> {
        return mapOf(
            "status" to booking.status.name,
            "paymentStatus" to booking.paymentStatus.name,
            "partySize" to booking.partySize,
            "offerExpiresAtUtc" to booking.offerExpiresAtUtc?.toString(),
            "waitlistSkipCount" to booking.waitlistSkipCount,
        )
    }
}
