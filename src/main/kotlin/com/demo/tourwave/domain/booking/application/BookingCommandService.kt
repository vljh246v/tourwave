package com.demo.tourwave.domain.booking.application

import com.demo.tourwave.domain.booking.Booking
import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import com.demo.tourwave.domain.common.IdempotencyDecision
import com.demo.tourwave.domain.common.IdempotencyStore
import com.demo.tourwave.domain.booking.repository.BookingRepository
import com.demo.tourwave.domain.occurrence.OccurrenceRepository
import com.demo.tourwave.domain.occurrence.OccurrenceStatus
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.Clock

@Service
class BookingCommandService(
    private val bookingRepository: BookingRepository,
    private val occurrenceRepository: OccurrenceRepository,
    private val idempotencyStore: IdempotencyStore,
    private val clock: Clock
) {
    fun createBooking(
        occurrenceId: Long,
        actorUserId: Long,
        idempotencyKey: String,
        request: BookingCreateRequest
    ): BookingCreateResult {
        validateCreateRequest(request)

        val pathTemplate = "/occurrences/{occurrenceId}/bookings"
        val requestHash = requestHash(occurrenceId, request)

        return when (
            val decision = idempotencyStore.reserveOrReplay(
                actorUserId = actorUserId,
                method = "POST",
                pathTemplate = pathTemplate,
                idempotencyKey = idempotencyKey,
                requestHash = requestHash
            )
        ) {
            is IdempotencyDecision.Replay -> BookingCreateResult(
                status = decision.status,
                body = decision.body as BookingResponse
            )

            IdempotencyDecision.Reserved -> {
                val occurrence = occurrenceRepository.getOrCreate(occurrenceId)

                if (occurrence.status == OccurrenceStatus.CANCELED) {
                    throw DomainException(
                        errorCode = ErrorCode.OCCURRENCE_ALREADY_CANCELED,
                        status = 409,
                        message = "Occurrence is already canceled",
                        details = mapOf("occurrenceId" to occurrenceId)
                    )
                }

                val unavailableSeats = bookingRepository.findByOccurrenceAndStatuses(
                    occurrenceId = occurrenceId,
                    statuses = setOf(BookingStatus.CONFIRMED, BookingStatus.OFFERED)
                ).sumOf { it.partySize }

                val availableSeats = (occurrence.capacity - unavailableSeats).coerceAtLeast(0)

                val created = bookingRepository.save(
                    Booking.create(
                        occurrenceId = occurrence.id,
                        organizationId = occurrence.organizationId,
                        leaderUserId = actorUserId,
                        partySize = request.partySize,
                        availableSeats = availableSeats
                    ).copy(createdAt = clock.instant())
                )

                val response = BookingResponse(
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
                    actorUserId = actorUserId,
                    method = "POST",
                    pathTemplate = pathTemplate,
                    idempotencyKey = idempotencyKey,
                    status = 201,
                    body = response
                )

                BookingCreateResult(status = 201, body = response)
            }
        }
    }

    private fun validateCreateRequest(request: BookingCreateRequest) {
        if (request.partySize !in 1..50) {
            throw DomainException(
                errorCode = ErrorCode.PARTY_SIZE_OUT_OF_RANGE,
                status = 422,
                message = "partySize must be between 1 and 50",
                details = mapOf("partySize" to request.partySize)
            )
        }
    }

    private fun requestHash(occurrenceId: Long, request: BookingCreateRequest): String {
        val raw = "$occurrenceId|${request.partySize}|${request.noteToOperator ?: ""}"
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
