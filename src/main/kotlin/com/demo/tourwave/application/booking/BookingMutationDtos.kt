package com.demo.tourwave.application.booking

enum class BookingMutationType(
    val pathTemplate: String
) {
    APPROVE("/bookings/{bookingId}/approve"),
    REJECT("/bookings/{bookingId}/reject"),
    CANCEL("/bookings/{bookingId}/cancel"),
    OFFER_ACCEPT("/bookings/{bookingId}/offer/accept"),
    OFFER_DECLINE("/bookings/{bookingId}/offer/decline")
}

data class MutateBookingCommand(
    val bookingId: Long,
    val actorUserId: Long,
    val idempotencyKey: String,
    val mutationType: BookingMutationType
)

data class MutateBookingResult(
    val status: Int
)
