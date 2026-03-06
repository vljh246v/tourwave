package com.demo.tourwave.adapter.`in`.web.booking

import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import java.time.Instant

data class BookingCreateWebRequest(
    val partySize: Int,
    val noteToOperator: String? = null
)

data class BookingPartySizeUpdateWebRequest(
    val partySize: Int
)

data class BookingCreateWebResponse(
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
