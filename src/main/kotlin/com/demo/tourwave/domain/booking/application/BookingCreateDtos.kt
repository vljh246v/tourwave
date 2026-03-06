package com.demo.tourwave.domain.booking.application

import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import java.time.Instant

data class BookingCreateRequest(
    val partySize: Int,
    val noteToOperator: String? = null
)

data class BookingResponse(
    val id: Long,
    val organizationId: Long,
    val occurrenceId: Long,
    val userId: Long,
    val partySize: Int,
    val status: BookingStatus,
    val paymentStatus: PaymentStatus,
    val currency: String? = null,
    val amountPaid: Int? = null,
    val createdAt: Instant
)

data class BookingCreateResult(
    val status: Int,
    val body: BookingResponse
)
