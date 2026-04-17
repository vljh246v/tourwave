package com.demo.tourwave.application.booking

import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import java.time.Instant

data class CreateBookingCommand(
    val occurrenceId: Long,
    val actorUserId: Long,
    val idempotencyKey: String,
    val partySize: Int,
    val noteToOperator: String? = null,
    val requestId: String? = null,
)

data class BookingCreated(
    val id: Long,
    val organizationId: Long,
    val occurrenceId: Long,
    val userId: Long,
    val partySize: Int,
    val status: BookingStatus,
    val paymentStatus: PaymentStatus,
    val currency: String? = null,
    val amountPaid: Int? = null,
    val createdAt: Instant,
)

data class CreateBookingResult(
    val status: Int,
    val booking: BookingCreated,
)
