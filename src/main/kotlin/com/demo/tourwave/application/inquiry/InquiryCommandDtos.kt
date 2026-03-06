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

data class PostInquiryMessageCommand(
    val inquiryId: Long,
    val actor: InquiryActorContext,
    val idempotencyKey: String,
    val body: String?,
    val attachmentAssetIds: List<Long>? = null
)

data class CloseInquiryCommand(
    val inquiryId: Long,
    val actor: InquiryActorContext,
    val idempotencyKey: String
)

data class ListInquiryMessagesQuery(
    val inquiryId: Long,
    val actor: InquiryActorContext,
    val cursor: String? = null,
    val limit: Int? = null
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

data class InquiryMessageView(
    val id: Long,
    val inquiryId: Long,
    val senderUserId: Long,
    val body: String,
    val attachmentAssetIds: List<Long>? = null,
    val createdAt: Instant
)

data class CreateInquiryResult(
    val status: Int,
    val inquiry: InquiryCreated
)

data class PostInquiryMessageResult(
    val status: Int,
    val message: InquiryMessageView
)

data class InquiryMessageListResult(
    val items: List<InquiryMessageView>,
    val nextCursor: String? = null
)

data class CloseInquiryResult(
    val status: Int
)
