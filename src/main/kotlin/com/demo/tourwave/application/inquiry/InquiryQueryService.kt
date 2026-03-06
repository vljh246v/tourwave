package com.demo.tourwave.application.inquiry

import com.demo.tourwave.application.inquiry.port.InquiryRepository
import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode

class InquiryQueryService(
    private val inquiryRepository: InquiryRepository,
    private val inquiryAccessPolicy: InquiryAccessPolicy
) {
    fun listMessages(query: ListInquiryMessagesQuery): InquiryMessageListResult {
        val inquiry = inquiryRepository.findById(query.inquiryId)
            ?: throw DomainException(
                errorCode = ErrorCode.VALIDATION_ERROR,
                status = 404,
                message = "Inquiry not found",
                details = mapOf("inquiryId" to query.inquiryId)
            )

        inquiryAccessPolicy.authorize(inquiry, query.actor)

        val messages = inquiryRepository.findMessagesByInquiryId(query.inquiryId)
            .map {
                InquiryMessageView(
                    id = requireNotNull(it.id),
                    inquiryId = it.inquiryId,
                    senderUserId = it.senderUserId,
                    body = it.body,
                    attachmentAssetIds = it.attachmentAssetIds,
                    createdAt = it.createdAt
                )
            }

        return InquiryMessageListResult(items = messages, nextCursor = null)
    }
}
