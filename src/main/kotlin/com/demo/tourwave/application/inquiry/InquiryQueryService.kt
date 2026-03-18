package com.demo.tourwave.application.inquiry

import com.demo.tourwave.application.inquiry.port.InquiryRepository
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode

class InquiryQueryService(
    private val inquiryRepository: InquiryRepository,
    private val inquiryAccessPolicy: InquiryAccessPolicy,
) {
    fun getInquiryDetail(query: GetInquiryDetailQuery): InquiryDetailView {
        val inquiry =
            inquiryRepository.findById(query.inquiryId)
                ?: throw DomainException(
                    errorCode = ErrorCode.VALIDATION_ERROR,
                    status = 404,
                    message = "Inquiry not found",
                    details = mapOf("inquiryId" to query.inquiryId),
                )

        inquiryAccessPolicy.authorize(inquiry, query.actor)

        return inquiry.toDetailView()
    }

    fun listMyInquiries(query: ListMyInquiriesQuery): InquiryListResult {
        val limit = normalizedLimit(query.limit)
        val cursorId = parseCursor(query.cursor, "cursor")
        val inquiries =
            inquiryRepository
                .findByCreatedByUserId(query.actorUserId)
                .asSequence()
                .filter { query.status == null || it.status == query.status }
                .filter { cursorId == null || requireNotNull(it.id) < cursorId }
                .map { it.toDetailView() }
                .toList()

        return InquiryListResult(
            items = inquiries.take(limit),
            nextCursor = inquiries.getOrNull(limit)?.id?.toString(),
        )
    }

    fun listMessages(query: ListInquiryMessagesQuery): InquiryMessageListResult {
        val inquiry =
            inquiryRepository.findById(query.inquiryId)
                ?: throw DomainException(
                    errorCode = ErrorCode.VALIDATION_ERROR,
                    status = 404,
                    message = "Inquiry not found",
                    details = mapOf("inquiryId" to query.inquiryId),
                )

        inquiryAccessPolicy.authorize(inquiry, query.actor)

        val limit = normalizedLimit(query.limit)
        val cursorId = parseCursor(query.cursor, "cursor")
        val messages =
            inquiryRepository
                .findMessagesByInquiryId(query.inquiryId)
                .asSequence()
                .filter { cursorId == null || requireNotNull(it.id) > cursorId }
                .map {
                    InquiryMessageView(
                        id = requireNotNull(it.id),
                        inquiryId = it.inquiryId,
                        senderUserId = it.senderUserId,
                        body = it.body,
                        attachmentAssetIds = it.attachmentAssetIds,
                        createdAt = it.createdAt,
                    )
                }.toList()

        return InquiryMessageListResult(
            items = messages.take(limit),
            nextCursor = messages.getOrNull(limit)?.id?.toString(),
        )
    }

    private fun com.demo.tourwave.domain.inquiry.Inquiry.toDetailView(): InquiryDetailView =
        InquiryDetailView(
            id = requireNotNull(id),
            organizationId = organizationId,
            occurrenceId = occurrenceId,
            bookingId = bookingId,
            createdByUserId = createdByUserId,
            subject = subject,
            status = status,
            createdAt = createdAt,
            lastMessageAt = inquiryRepository.findMessagesByInquiryId(id).maxOfOrNull { it.createdAt },
        )

    private fun normalizedLimit(limit: Int?): Int {
        if (limit == null) return 20
        if (limit !in 1..100) {
            throw DomainException(
                errorCode = ErrorCode.VALIDATION_ERROR,
                status = 422,
                message = "limit must be between 1 and 100",
                details = mapOf("limit" to limit),
            )
        }
        return limit
    }

    private fun parseCursor(
        cursor: String?,
        fieldName: String,
    ): Long? {
        if (cursor == null) return null
        return cursor.toLongOrNull() ?: throw DomainException(
            errorCode = ErrorCode.VALIDATION_ERROR,
            status = 422,
            message = "$fieldName must be a numeric cursor",
            details = mapOf(fieldName to cursor),
        )
    }
}
