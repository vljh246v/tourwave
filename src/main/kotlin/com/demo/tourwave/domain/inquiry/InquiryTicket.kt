package com.demo.tourwave.domain.inquiry

data class InquiryTicket(
    val id: Long? = null,
    val organizationId: Long,
    val occurrenceId: Long,
    val bookingId: Long,
    val createdByUserId: Long,
    val subject: String? = null,
    val status: InquiryStatus = InquiryStatus.OPEN
) {
    fun close(): InquiryTicket {
        if (status == InquiryStatus.CLOSED) return this
        return copy(status = InquiryStatus.CLOSED)
    }
}

