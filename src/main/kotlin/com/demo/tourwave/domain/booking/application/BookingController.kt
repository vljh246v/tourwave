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
}
