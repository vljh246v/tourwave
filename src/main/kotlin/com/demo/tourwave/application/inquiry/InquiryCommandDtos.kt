package com.demo.tourwave.application.inquiry

import com.demo.tourwave.domain.inquiry.InquiryStatus
import java.time.Instant

data class CreateInquiryCommand(
    val occurrenceId: Long,
    val actorUserId: Long,
    val idempotencyKey: String,
    val bookingId: Long?,
    val subject: String? = null,
    val message: String,
    val requestId: String? = null
)

data class InquiryCreated(
    val id: Long,
    val organizationId: Long,
    val occurrenceId: Long,
    val bookingId: Long,
    val createdByUserId: Long,
    val subject: String? = null,
    val status: InquiryStatus,
    val createdAt: Instant
)

data class CreateInquiryResult(
    val status: Int,
    val inquiry: InquiryCreated
)

