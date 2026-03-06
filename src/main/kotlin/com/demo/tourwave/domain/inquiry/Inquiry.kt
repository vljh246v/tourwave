package com.demo.tourwave.domain.inquiry

import java.time.Instant

data class Inquiry(
    val id: Long? = null,
    val organizationId: Long,
    val occurrenceId: Long,
    val bookingId: Long,
    val createdByUserId: Long,
    val subject: String? = null,
    val status: InquiryStatus = InquiryStatus.OPEN,
    val createdAt: Instant = Instant.now()
) {
    fun close(): Inquiry {
        if (status == InquiryStatus.CLOSED) return this
        return copy(status = InquiryStatus.CLOSED)
    }
}
