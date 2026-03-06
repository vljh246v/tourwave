package com.demo.tourwave.domain.booking.application

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class BookingController(
    private val bookingCommandService: BookingCommandService
) {
    @PostMapping("/occurrences/{occurrenceId}/bookings")
    fun createBooking(
        @PathVariable occurrenceId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestBody request: BookingCreateRequest
    ): ResponseEntity<BookingResponse> {
        val result = bookingCommandService.createBooking(
            occurrenceId = occurrenceId,
            actorUserId = actorUserId ?: 1L,
            idempotencyKey = idempotencyKey,
            request = request
        )
        return ResponseEntity.status(result.status).body(result.body)
    }

    @PostMapping("/bookings/{bookingId}/approve")
    fun approveBooking(
        @PathVariable bookingId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?
    ): ResponseEntity<Void> {
        val result = bookingCommandService.mutateBooking(
            bookingId = bookingId,
            actorUserId = actorUserId ?: 1L,
            idempotencyKey = idempotencyKey,
            mutationType = BookingMutationType.APPROVE
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
            bookingId = bookingId,
            actorUserId = actorUserId ?: 1L,
            idempotencyKey = idempotencyKey,
            mutationType = BookingMutationType.REJECT
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
            bookingId = bookingId,
            actorUserId = actorUserId ?: 1L,
            idempotencyKey = idempotencyKey,
            mutationType = BookingMutationType.CANCEL
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
            bookingId = bookingId,
            actorUserId = actorUserId ?: 1L,
            idempotencyKey = idempotencyKey,
            mutationType = BookingMutationType.OFFER_ACCEPT
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
            bookingId = bookingId,
            actorUserId = actorUserId ?: 1L,
            idempotencyKey = idempotencyKey,
            mutationType = BookingMutationType.OFFER_DECLINE
        )
        return ResponseEntity.status(result.status).build()
    }
}
