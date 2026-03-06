package com.demo.tourwave.domain.booking.application

enum class BookingMutationType(
    val pathTemplate: String
) {
    APPROVE("/bookings/{bookingId}/approve"),
    REJECT("/bookings/{bookingId}/reject"),
    CANCEL("/bookings/{bookingId}/cancel"),
    OFFER_ACCEPT("/bookings/{bookingId}/offer/accept"),
    OFFER_DECLINE("/bookings/{bookingId}/offer/decline")
}

data class BookingMutationResult(
    val status: Int
)

