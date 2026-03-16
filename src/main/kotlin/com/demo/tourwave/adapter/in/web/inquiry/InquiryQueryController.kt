package com.demo.tourwave.adapter.`in`.web.inquiry

import com.demo.tourwave.application.common.port.ActorAuthContext
import com.demo.tourwave.application.common.port.AuthzGuardPort
import com.demo.tourwave.application.inquiry.GetInquiryDetailQuery
import com.demo.tourwave.application.inquiry.InquiryActorContext
import com.demo.tourwave.application.inquiry.InquiryDetailView
import com.demo.tourwave.application.inquiry.InquiryListResult
import com.demo.tourwave.application.inquiry.InquiryMessageListResult
import com.demo.tourwave.application.inquiry.InquiryMessageView
import com.demo.tourwave.application.inquiry.InquiryQueryService
import com.demo.tourwave.application.inquiry.ListInquiryMessagesQuery
import com.demo.tourwave.application.inquiry.ListMyInquiriesQuery
import com.demo.tourwave.domain.inquiry.InquiryStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class InquiryQueryController(
    private val inquiryQueryService: InquiryQueryService,
    private val authzGuardPort: AuthzGuardPort
) {
    @GetMapping("/inquiries/{inquiryId}")
    fun getInquiryDetail(
        @PathVariable inquiryId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("X-Actor-Org-Role", required = false) actorOrgRole: String?,
        @RequestHeader("X-Actor-Org-Id", required = false) actorOrgId: Long?,
        @RequestHeader("X-Request-Id", required = false) requestId: String?
    ): ResponseEntity<InquiryDetailWebResponse> {
        val actor = authzGuardPort.requireActorContext(
            actorUserId = actorUserId,
            actorOrgRole = actorOrgRole,
            actorOrgId = actorOrgId
        )

        val result = inquiryQueryService.getInquiryDetail(
            GetInquiryDetailQuery(
                inquiryId = inquiryId,
                actor = actor.toInquiryActorContext(requestId)
            )
        )
        return ResponseEntity.ok(result.toWebResponse())
    }

    @GetMapping("/me/inquiries")
    fun listMyInquiries(
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestParam(required = false) status: InquiryStatus?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) limit: Int?
    ): ResponseEntity<InquiryListWebResponse> {
        val requiredActorUserId = authzGuardPort.requireActorUserId(actorUserId)
        val result = inquiryQueryService.listMyInquiries(
            ListMyInquiriesQuery(
                actorUserId = requiredActorUserId,
                status = status,
                cursor = cursor,
                limit = limit
            )
        )
        return ResponseEntity.ok(result.toWebResponse())
    }

    @GetMapping("/inquiries/{inquiryId}/messages")
    fun listInquiryMessages(
        @PathVariable inquiryId: Long,
        @RequestHeader("X-Actor-User-Id", required = false) actorUserId: Long?,
        @RequestHeader("X-Actor-Org-Role", required = false) actorOrgRole: String?,
        @RequestHeader("X-Actor-Org-Id", required = false) actorOrgId: Long?,
        @RequestHeader("X-Request-Id", required = false) requestId: String?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) limit: Int?
    ): ResponseEntity<InquiryMessageListWebResponse> {
        val actor = authzGuardPort.requireActorContext(
            actorUserId = actorUserId,
            actorOrgRole = actorOrgRole,
            actorOrgId = actorOrgId
        )
        val result = inquiryQueryService.listMessages(
            ListInquiryMessagesQuery(
                inquiryId = inquiryId,
                actor = actor.toInquiryActorContext(requestId),
                cursor = cursor,
                limit = limit
            )
        )
        return ResponseEntity.ok(result.toWebResponse())
    }

    private fun ActorAuthContext.toInquiryActorContext(requestId: String?): InquiryActorContext {
        return InquiryActorContext(
            actorUserId = actorUserId,
            actorOrgRole = actorOrgRole,
            actorOrgId = actorOrgId,
            requestId = requestId
        )
    }

    private fun InquiryDetailView.toWebResponse(): InquiryDetailWebResponse {
        return InquiryDetailWebResponse(
            id = id,
            organizationId = organizationId,
            occurrenceId = occurrenceId,
            bookingId = bookingId,
            createdByUserId = createdByUserId,
            subject = subject,
            status = status,
            createdAt = createdAt,
            lastMessageAt = lastMessageAt
        )
    }

    private fun InquiryListResult.toWebResponse(): InquiryListWebResponse {
        return InquiryListWebResponse(
            items = items.map { it.toWebResponse() },
            nextCursor = nextCursor
        )
    }

    private fun InquiryMessageListResult.toWebResponse(): InquiryMessageListWebResponse {
        return InquiryMessageListWebResponse(
            items = items.map { it.toWebResponse() },
            nextCursor = nextCursor
        )
    }

    private fun InquiryMessageView.toWebResponse(): InquiryMessageWebResponse {
        return InquiryMessageWebResponse(
            id = id,
            inquiryId = inquiryId,
            senderUserId = senderUserId,
            body = body,
            attachmentAssetIds = attachmentAssetIds,
            createdAt = createdAt
        )
    }
}
