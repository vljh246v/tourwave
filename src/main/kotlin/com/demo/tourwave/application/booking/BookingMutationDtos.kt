package com.demo.tourwave.application.booking

import com.demo.tourwave.domain.booking.Booking

enum class BookingMutationType(
    val httpMethod: String,
    val pathTemplate: String,
) {
    APPROVE("POST", "/bookings/{bookingId}/approve"),
    REJECT("POST", "/bookings/{bookingId}/reject"),
    CANCEL("POST", "/bookings/{bookingId}/cancel"),
    OFFER_ACCEPT("POST", "/bookings/{bookingId}/offer/accept"),
    OFFER_DECLINE("POST", "/bookings/{bookingId}/offer/decline"),
    PARTY_SIZE_PATCH("PATCH", "/bookings/{bookingId}/party-size"),
    COMPLETE("POST", "/bookings/{bookingId}/complete"),
}

data class MutateBookingCommand(
    val bookingId: Long,
    val actorUserId: Long,
    val idempotencyKey: String,
    val mutationType: BookingMutationType,
    val partySize: Int? = null,
    val requestId: String? = null,
)

data class MutateBookingResult(
    val status: Int,
    val booking: Booking? = null,
)
