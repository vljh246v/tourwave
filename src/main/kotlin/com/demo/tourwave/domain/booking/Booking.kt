package com.demo.tourwave.domain.booking

import java.time.Instant

data class Booking(
    val id: Long? = null,
    val occurrenceId: Long,
    val organizationId: Long,
    val leaderUserId: Long,
    val partySize: Int,
    val status: BookingStatus,
    val paymentStatus: PaymentStatus,
    val offerExpiresAtUtc: Instant? = null
) {
    init {
        require(partySize >= 1) { "partySize must be >= 1" }
    }

    companion object {
        fun create(
            occurrenceId: Long,
            organizationId: Long,
            leaderUserId: Long,
            partySize: Int,
            availableSeats: Int
        ): Booking {
            require(availableSeats >= 0) { "availableSeats must be >= 0" }
            val initialStatus = if (availableSeats >= partySize) BookingStatus.REQUESTED else BookingStatus.WAITLISTED
            return Booking(
                occurrenceId = occurrenceId,
                organizationId = organizationId,
                leaderUserId = leaderUserId,
                partySize = partySize,
                status = initialStatus,
                paymentStatus = PaymentStatus.AUTHORIZED
            )
        }
    }

    fun offer(expiresAtUtc: Instant): Booking {
        require(status == BookingStatus.WAITLISTED) { "Only WAITLISTED booking can be offered" }
        return copy(status = BookingStatus.OFFERED, offerExpiresAtUtc = expiresAtUtc)
    }

    fun acceptOffer(now: Instant): Booking {
        require(status == BookingStatus.OFFERED) { "Offer is not active" }
        require(offerExpiresAtUtc != null) { "offerExpiresAtUtc is required for OFFERED booking" }
        require(!now.isAfter(offerExpiresAtUtc)) { "Offer is expired" }
        return copy(status = BookingStatus.CONFIRMED, paymentStatus = PaymentStatus.PAID)
    }

    fun declineOffer(now: Instant): Booking {
        require(status == BookingStatus.OFFERED) { "Offer is not active" }
        require(offerExpiresAtUtc != null) { "offerExpiresAtUtc is required for OFFERED booking" }
        require(!now.isAfter(offerExpiresAtUtc)) { "Offer is expired" }
        return copy(status = BookingStatus.EXPIRED, paymentStatus = PaymentStatus.REFUNDED)
    }

    fun expireOffer(): Booking {
        require(status == BookingStatus.OFFERED) { "Only OFFERED booking can be expired" }
        return copy(status = BookingStatus.EXPIRED)
    }

    fun approve(): Booking {
        require(status == BookingStatus.REQUESTED) { "Only REQUESTED booking can be approved" }
        return copy(status = BookingStatus.CONFIRMED, paymentStatus = PaymentStatus.PAID)
    }

    fun reject(): Booking {
        require(status == BookingStatus.REQUESTED) { "Only REQUESTED booking can be rejected" }
        return copy(status = BookingStatus.REJECTED, paymentStatus = PaymentStatus.REFUNDED)
    }

    fun cancel(refund: Boolean): Booking {
        require(!status.isTerminal()) { "Terminal booking cannot be canceled" }
        return copy(
            status = BookingStatus.CANCELED,
            paymentStatus = if (refund) PaymentStatus.REFUNDED else paymentStatus
        )
    }

    fun complete(): Booking {
        require(status == BookingStatus.CONFIRMED) { "Only CONFIRMED booking can be completed" }
        return copy(status = BookingStatus.COMPLETED)
    }

    fun reducePartySize(newPartySize: Int): Booking {
        require(newPartySize >= 1) { "partySize must be >= 1" }
        require(newPartySize <= partySize) { "partySize can only be decreased" }
        return copy(partySize = newPartySize)
    }
}

