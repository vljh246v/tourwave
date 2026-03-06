package com.demo.tourwave.domain.booking

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

class BookingTest {

    @Test
    fun `create should set REQUESTED when seats are enough`() {
        val booking = Booking.create(
            occurrenceId = 1L,
            organizationId = 10L,
            leaderUserId = 100L,
            partySize = 2,
            availableSeats = 2
        )

        assertEquals(BookingStatus.REQUESTED, booking.status)
        assertEquals(PaymentStatus.AUTHORIZED, booking.paymentStatus)
    }

    @Test
    fun `create should set WAITLISTED when seats are insufficient`() {
        val booking = Booking.create(
            occurrenceId = 1L,
            organizationId = 10L,
            leaderUserId = 100L,
            partySize = 3,
            availableSeats = 2
        )

        assertEquals(BookingStatus.WAITLISTED, booking.status)
    }

    @Test
    fun `acceptOffer should fail when offer is expired`() {
        val now = Instant.parse("2026-03-06T10:00:01Z")
        val expiredAt = Instant.parse("2026-03-06T10:00:00Z")
        val offered = Booking.create(1L, 10L, 100L, 2, 0).offer(expiredAt)

        assertThrows<IllegalArgumentException> {
            offered.acceptOffer(now)
        }
    }

    @Test
    fun `acceptOffer should allow when now equals expiresAt`() {
        val now = Instant.parse("2026-03-06T10:00:00Z")
        val offered = Booking.create(1L, 10L, 100L, 2, 0).offer(now)

        val accepted = offered.acceptOffer(now)

        assertEquals(BookingStatus.CONFIRMED, accepted.status)
    }

    @Test
    fun `reducePartySize should reject increase`() {
        val booking = Booking.create(1L, 10L, 100L, 2, 10)

        assertThrows<IllegalArgumentException> {
            booking.decreasePartySize(3)
        }
    }
}
