package com.demo.tourwave.adapter.`in`.web.booking

import com.demo.tourwave.application.booking.BookingCommandService
import com.demo.tourwave.application.booking.BookingCreated
import com.demo.tourwave.application.booking.BookingMutationType
import com.demo.tourwave.application.booking.CreateBookingCommand
import com.demo.tourwave.application.booking.MutateBookingCommand
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class BookingCommandController(
    private val bookingCommandService: BookingCommandService
) {
    @PostMapping("/occurrences/{occurrenceId}/bookings")
    fun createBooking(
        @PathVariable occurrenceId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestBody request: BookingCreateWebRequest
    ): ResponseEntity<BookingCreateWebResponse> {
        val result = bookingCommandService.createBooking(
            CreateBookingCommand(
                occurrenceId = occurrenceId,
                actorUserId = actorUserId ?: 1L,
                idempotencyKey = idempotencyKey,
                partySize = request.partySize,
                noteToOperator = request.noteToOperator
            )
        )
        return ResponseEntity.status(result.status).body(result.booking.toWebResponse())
    }

    @PostMapping("/bookings/{bookingId}/approve")
    fun approveBooking(
        @PathVariable bookingId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?
    ): ResponseEntity<Void> {
        val result = bookingCommandService.mutateBooking(
            MutateBookingCommand(
                bookingId = bookingId,
                actorUserId = actorUserId ?: 1L,
                idempotencyKey = idempotencyKey,
                mutationType = BookingMutationType.APPROVE
            )
        )
        return ResponseEntity.status(result.status).build()
    }

    @PostMapping("/bookings/{bookingId}/reject")
    fun rejectBooking(
        @PathVariable bookingId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?
    ): ResponseEntity<Void> {
        val result = bookingCommandService.mutateBooking(
            MutateBookingCommand(
                bookingId = bookingId,
                actorUserId = actorUserId ?: 1L,
                idempotencyKey = idempotencyKey,
                mutationType = BookingMutationType.REJECT
            )
        )
        return ResponseEntity.status(result.status).build()
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    fun cancelBooking(
        @PathVariable bookingId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?
    ): ResponseEntity<Void> {
        val result = bookingCommandService.mutateBooking(
            MutateBookingCommand(
                bookingId = bookingId,
                actorUserId = actorUserId ?: 1L,
                idempotencyKey = idempotencyKey,
                mutationType = BookingMutationType.CANCEL
            )
        )
        return ResponseEntity.status(result.status).build()
    }

    @PostMapping("/bookings/{bookingId}/offer/accept")
    fun acceptOffer(
        @PathVariable bookingId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?
    ): ResponseEntity<Void> {
        val result = bookingCommandService.mutateBooking(
            MutateBookingCommand(
                bookingId = bookingId,
                actorUserId = actorUserId ?: 1L,
                idempotencyKey = idempotencyKey,
                mutationType = BookingMutationType.OFFER_ACCEPT
            )
        )
        return ResponseEntity.status(result.status).build()
    }

    @PostMapping("/bookings/{bookingId}/offer/decline")
    fun declineOffer(
        @PathVariable bookingId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?
    ): ResponseEntity<Void> {
        val result = bookingCommandService.mutateBooking(
            MutateBookingCommand(
                bookingId = bookingId,
                actorUserId = actorUserId ?: 1L,
                idempotencyKey = idempotencyKey,
                mutationType = BookingMutationType.OFFER_DECLINE
            )
        )
        return ResponseEntity.status(result.status).build()
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
}
