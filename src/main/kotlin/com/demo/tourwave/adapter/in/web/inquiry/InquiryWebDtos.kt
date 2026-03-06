package com.demo.tourwave.adapter.`in`.web.inquiry

import com.demo.tourwave.domain.inquiry.InquiryStatus
import java.time.Instant

data class InquiryCreateWebRequest(
    val bookingId: Long? = null,
    val subject: String? = null,
    val message: String
)

data class InquiryMessageCreateWebRequest(
    val body: String? = null,
    val attachmentAssetIds: List<Long>? = null
)

data class InquiryMessageWebResponse(
    val id: Long,
    val inquiryId: Long,
    val senderUserId: Long,
    val body: String,
    val attachmentAssetIds: List<Long>? = null,
    val createdAt: Instant
)

data class InquiryMessageListWebResponse(
    val items: List<InquiryMessageWebResponse>,
    val nextCursor: String? = null
)

data class InquiryWebResponse(
    val id: Long,
    val organizationId: Long,
    val occurrenceId: Long,
    val bookingId: Long,
    val createdByUserId: Long,
    val subject: String? = null,
    val status: InquiryStatus,
    val createdAt: Instant
)
