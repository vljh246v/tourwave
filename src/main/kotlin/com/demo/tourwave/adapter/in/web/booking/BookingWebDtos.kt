package com.demo.tourwave.adapter.`in`.web.booking

import com.demo.tourwave.domain.booking.BookingStatus
import com.demo.tourwave.domain.booking.PaymentStatus
import com.demo.tourwave.domain.booking.AttendanceStatus
import com.demo.tourwave.domain.occurrence.OccurrenceStatus
import com.demo.tourwave.domain.participant.BookingParticipantStatus
import com.demo.tourwave.domain.booking.RefundDecisionType
import com.demo.tourwave.domain.booking.RefundReasonCode
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

data class BookingOccurrenceWebResponse(
    val id: Long,
    val organizationId: Long,
    val tourId: Long?,
    val instructorProfileId: Long?,
    val capacity: Int,
    val startsAtUtc: Instant?,
    val status: OccurrenceStatus
)

data class BookingDetailParticipantWebResponse(
    val id: Long,
    val bookingId: Long,
    val userId: Long,
    val status: BookingParticipantStatus,
    val attendanceStatus: AttendanceStatus,
    val invitedAt: Instant?,
    val respondedAt: Instant?
)

data class BookingDetailWebResponse(
    val id: Long,
    val organizationId: Long,
    val occurrenceId: Long,
    val userId: Long,
    val partySize: Int,
    val status: BookingStatus,
    val paymentStatus: PaymentStatus,
    val createdAt: Instant,
    val offerExpiresAtUtc: Instant?,
    val occurrence: BookingOccurrenceWebResponse,
    val participants: List<BookingDetailParticipantWebResponse>
)

data class BookingRefundPreviewWebResponse(
    val bookingId: Long,
    val cancelable: Boolean,
    val status: BookingStatus,
    val paymentStatus: PaymentStatus,
    val refundDecisionType: RefundDecisionType?,
    val refundReasonCode: RefundReasonCode?,
    val refundable: Boolean,
    val occurrenceStartsAtUtc: Instant?,
    val evaluatedAtUtc: Instant
)
