package com.demo.tourwave.adapter.`in`.web.booking

import com.demo.tourwave.application.common.port.AuthzGuardPort
import com.demo.tourwave.application.booking.BookingCommandService
import com.demo.tourwave.application.booking.BookingCreated
import com.demo.tourwave.application.booking.BookingMutationType
import com.demo.tourwave.application.booking.CancelOccurrenceCommand
import com.demo.tourwave.application.booking.CreateBookingCommand
import com.demo.tourwave.application.booking.FinishOccurrenceCommand
import com.demo.tourwave.application.booking.MutateBookingCommand
import com.demo.tourwave.domain.booking.Booking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class BookingCommandController(
    private val bookingCommandService: BookingCommandService,
    private val authzGuardPort: AuthzGuardPort
) {
    @PostMapping("/occurrences/{occurrenceId}/bookings")
    fun createBooking(
        @PathVariable occurrenceId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("X-Request-Id", required = false) requestId: String?,
        @RequestBody request: BookingCreateWebRequest
    ): ResponseEntity<BookingCreateWebResponse> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        val result = bookingCommandService.createBooking(
            CreateBookingCommand(
                occurrenceId = occurrenceId,
                actorUserId = requiredActorUserId,
                idempotencyKey = idempotencyKey,
                partySize = request.partySize,
                noteToOperator = request.noteToOperator,
                requestId = requestId
            )
        )
        return ResponseEntity.status(result.status).body(result.booking.toWebResponse())
    }

    @PostMapping("/occurrences/{occurrenceId}/cancel")
    fun cancelOccurrence(
        @PathVariable occurrenceId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("X-Request-Id", required = false) requestId: String?
    ): ResponseEntity<Void> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        val result = bookingCommandService.cancelOccurrence(
            CancelOccurrenceCommand(
                occurrenceId = occurrenceId,
                actorUserId = requiredActorUserId,
                idempotencyKey = idempotencyKey,
                requestId = requestId
            )
        )
        return ResponseEntity.status(result.status).build()
    }

    @PostMapping("/occurrences/{occurrenceId}/finish")
    fun finishOccurrence(
        @PathVariable occurrenceId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("X-Request-Id", required = false) requestId: String?
    ): ResponseEntity<Void> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        val result = bookingCommandService.finishOccurrence(
            FinishOccurrenceCommand(
                occurrenceId = occurrenceId,
                actorUserId = requiredActorUserId,
                idempotencyKey = idempotencyKey,
                requestId = requestId
            )
        )
        return ResponseEntity.status(result.status).build()
    }

    @PostMapping("/bookings/{bookingId}/approve")
    fun approveBooking(
        @PathVariable bookingId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("X-Request-Id", required = false) requestId: String?
    ): ResponseEntity<Void> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        val result = bookingCommandService.mutateBooking(
            MutateBookingCommand(
                bookingId = bookingId,
                actorUserId = requiredActorUserId,
                idempotencyKey = idempotencyKey,
                mutationType = BookingMutationType.APPROVE,
                requestId = requestId
            )
        )
        return ResponseEntity.status(result.status).build()
    }

    @PostMapping("/bookings/{bookingId}/reject")
    fun rejectBooking(
        @PathVariable bookingId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("X-Request-Id", required = false) requestId: String?
    ): ResponseEntity<Void> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        val result = bookingCommandService.mutateBooking(
            MutateBookingCommand(
                bookingId = bookingId,
                actorUserId = requiredActorUserId,
                idempotencyKey = idempotencyKey,
                mutationType = BookingMutationType.REJECT,
                requestId = requestId
            )
        )
        return ResponseEntity.status(result.status).build()
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    fun cancelBooking(
        @PathVariable bookingId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("X-Request-Id", required = false) requestId: String?
    ): ResponseEntity<Void> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        val result = bookingCommandService.mutateBooking(
            MutateBookingCommand(
                bookingId = bookingId,
                actorUserId = requiredActorUserId,
                idempotencyKey = idempotencyKey,
                mutationType = BookingMutationType.CANCEL,
                requestId = requestId
            )
        )
        return ResponseEntity.status(result.status).build()
    }

    @PostMapping("/bookings/{bookingId}/complete")
    fun completeBooking(
        @PathVariable bookingId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("X-Request-Id", required = false) requestId: String?
    ): ResponseEntity<Void> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        val result = bookingCommandService.mutateBooking(
            MutateBookingCommand(
                bookingId = bookingId,
                actorUserId = requiredActorUserId,
                idempotencyKey = idempotencyKey,
                mutationType = BookingMutationType.COMPLETE,
                requestId = requestId
            )
        )
        return ResponseEntity.status(result.status).build()
    }

    @PostMapping("/bookings/{bookingId}/offer/accept")
    fun acceptOffer(
        @PathVariable bookingId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("X-Request-Id", required = false) requestId: String?
    ): ResponseEntity<Void> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        val result = bookingCommandService.mutateBooking(
            MutateBookingCommand(
                bookingId = bookingId,
                actorUserId = requiredActorUserId,
                idempotencyKey = idempotencyKey,
                mutationType = BookingMutationType.OFFER_ACCEPT,
                requestId = requestId
            )
        )
        return ResponseEntity.status(result.status).build()
    }

    @PostMapping("/bookings/{bookingId}/offer/decline")
    fun declineOffer(
        @PathVariable bookingId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("X-Request-Id", required = false) requestId: String?
    ): ResponseEntity<Void> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        val result = bookingCommandService.mutateBooking(
            MutateBookingCommand(
                bookingId = bookingId,
                actorUserId = requiredActorUserId,
                idempotencyKey = idempotencyKey,
                mutationType = BookingMutationType.OFFER_DECLINE,
                requestId = requestId
            )
        )
        return ResponseEntity.status(result.status).build()
    }

    @PatchMapping("/bookings/{bookingId}/party-size")
    fun patchPartySize(
        @PathVariable bookingId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("X-Request-Id", required = false) requestId: String?,
        @RequestBody request: BookingPartySizeUpdateWebRequest
    ): ResponseEntity<BookingCreateWebResponse> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        val result = bookingCommandService.mutateBooking(
            MutateBookingCommand(
                bookingId = bookingId,
                actorUserId = requiredActorUserId,
                idempotencyKey = idempotencyKey,
                mutationType = BookingMutationType.PARTY_SIZE_PATCH,
                partySize = request.partySize,
                requestId = requestId
            )
        )

        return ResponseEntity.status(result.status).body(requireNotNull(result.booking).toWebResponse())
    }

    private fun BookingCreated.toWebResponse(): BookingCreateWebResponse {
        return BookingCreateWebResponse(
            id = id,
            organizationId = organizationId,
            occurrenceId = occurrenceId,
            userId = userId,
            partySize = partySize,
            status = status,
            paymentStatus = paymentStatus,
            currency = currency,
            amountPaid = amountPaid,
            createdAt = createdAt
        )
    }

    private fun Booking.toWebResponse(): BookingCreateWebResponse {
        return BookingCreateWebResponse(
            id = requireNotNull(id),
            organizationId = organizationId,
            occurrenceId = occurrenceId,
            userId = leaderUserId,
            partySize = partySize,
            status = status,
            paymentStatus = paymentStatus,
            createdAt = createdAt
        )
    }
}
